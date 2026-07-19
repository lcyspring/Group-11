package com.meession.etm.module.crm.service.activity;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.activity.vo.*;
import com.meession.etm.module.crm.dal.dataobject.activity.*;
import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueDO;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.mysql.activity.*;
import com.meession.etm.module.crm.dal.mysql.clue.CrmClueMapper;
import com.meession.etm.module.crm.dal.mysql.contact.CrmContactMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.enums.activity.*;
import com.meession.etm.module.crm.enums.clue.CrmCluePoolStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.activity.CrmActivityProperties;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

@Service
@Validated
public class CrmActivityServiceImpl implements CrmActivityService {
    @Resource private CrmTaskMapper taskMapper;
    @Resource private CrmTaskActionRecordMapper taskActionRecordMapper;
    @Resource private CrmCallRecordMapper callRecordMapper;
    @Resource private CrmSmsRecordMapper smsRecordMapper;
    @Resource private CrmClueConversionRecordMapper conversionRecordMapper;
    @Resource private CrmClueMapper clueMapper;
    @Resource private CrmCustomerMapper customerMapper;
    @Resource private CrmContactMapper contactMapper;
    @Resource private CrmPermissionService permissionService;
    @Resource private AdminUserApi adminUserApi;
    @Resource private CrmActivityProperties properties;
    @Resource private CrmActivityNotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizTypeValue = "#reqVO.bizType", bizId = "#reqVO.bizId",
            level = CrmPermissionLevelEnum.WRITE)
    public Long createTask(CrmTaskSaveReqVO reqVO, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        validateActivityObject(reqVO.getBizType(), reqVO.getBizId(), true);
        validateTaskFields(reqVO, now);
        adminUserApi.validateUser(reqVO.getAssigneeUserId());
        CrmTaskDO task = BeanUtils.toBean(reqVO, CrmTaskDO.class)
                .setId(null).setSourceClueId(null).setStatus(CrmTaskStatusEnum.NOT_STARTED.getStatus())
                .setVersion(0).setStartTime(null).setFinishTime(null).setResult(null);
        task.setCreator(String.valueOf(userId));
        taskMapper.insert(task);
        appendTaskRecord(task, CrmTaskActionTypeEnum.CREATE, null, task.getStatus(), userId, null);
        notificationService.notifyAssigned(task);
        return task.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTask(CrmTaskSaveReqVO reqVO, Long userId) {
        if (reqVO.getId() == null) throw exception(TASK_NOT_EXISTS);
        CrmTaskDO old = requireTaskForUpdate(reqVO.getId(), userId);
        if (!Objects.equals(old.getBizType(), reqVO.getBizType()) || !Objects.equals(old.getBizId(), reqVO.getBizId())
                || !Objects.equals(old.getCreator(), String.valueOf(userId))
                || !Objects.equals(old.getStatus(), CrmTaskStatusEnum.NOT_STARTED.getStatus())) {
            throw exception(TASK_EDIT_DENIED);
        }
        validateTaskFields(reqVO, LocalDateTime.now());
        adminUserApi.validateUser(reqVO.getAssigneeUserId());
        CrmTaskDO update = BeanUtils.toBean(reqVO, CrmTaskDO.class).setVersion(old.getVersion());
        if (taskMapper.updateDraft(update, userId) != 1) throw exception(TASK_TRANSITION_INVALID);
        appendTaskRecord(old, CrmTaskActionTypeEnum.UPDATE, old.getStatus(), old.getStatus(), userId,
                "修改任务内容");
        if (!Objects.equals(old.getAssigneeUserId(), reqVO.getAssigneeUserId())) {
            notificationService.notifyAssigned(update);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startTask(CrmTaskActionReqVO reqVO, Long userId) {
        CrmTaskDO task = requireTaskForUpdate(reqVO.getId(), userId);
        requireAssignee(task, userId);
        LocalDateTime now = LocalDateTime.now();
        if (taskMapper.startIfStartable(task.getId(), userId, now) != 1) throw exception(TASK_TRANSITION_INVALID);
        appendTaskRecord(task, CrmTaskActionTypeEnum.START, task.getStatus(),
                CrmTaskStatusEnum.IN_PROGRESS.getStatus(), userId, trimToNull(reqVO.getRemark()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(CrmTaskActionReqVO reqVO, Long userId) {
        finishTask(reqVO, userId, CrmTaskStatusEnum.COMPLETED, CrmTaskActionTypeEnum.COMPLETE, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markTaskUnable(CrmTaskActionReqVO reqVO, Long userId) {
        finishTask(reqVO, userId, CrmTaskStatusEnum.NOT_COMPLETED, CrmTaskActionTypeEnum.UNABLE, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(CrmTaskActionReqVO reqVO, Long userId) {
        CrmTaskDO task = requireTaskForUpdate(reqVO.getId(), userId);
        if (!Objects.equals(task.getCreator(), String.valueOf(userId))
                && !Objects.equals(task.getAssigneeUserId(), userId)) throw exception(TASK_EDIT_DENIED);
        String reason = trimToNull(reqVO.getRemark());
        if (reason == null) throw exception(TASK_RESULT_REQUIRED);
        finishOpenTask(task, CrmTaskStatusEnum.CANCELED, CrmTaskActionTypeEnum.CANCEL, reason, userId);
    }

    @Override
    @CrmPermission(bizTypeValue = "#reqVO.bizType", bizId = "#reqVO.bizId",
            level = CrmPermissionLevelEnum.READ)
    public PageResult<CrmTaskDO> getTaskPage(CrmTaskPageReqVO reqVO) {
        validateActivityType(reqVO.getBizType());
        return taskMapper.selectPage(reqVO);
    }

    @Override
    public List<CrmTaskActionRecordDO> getTaskActionRecords(Long taskId, Long userId) {
        CrmTaskDO task = taskMapper.selectById(taskId);
        if (task == null) throw exception(TASK_NOT_EXISTS);
        if (!permissionService.hasPermission(task.getBizType(), task.getBizId(), userId,
                CrmPermissionLevelEnum.READ)) throw exception(CRM_PERMISSION_DENIED, "CRM 任务");
        return taskActionRecordMapper.selectListByTaskIds(List.of(taskId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizTypeValue = "#reqVO.bizType", bizId = "#reqVO.bizId",
            level = CrmPermissionLevelEnum.WRITE)
    public Long createCallRecord(CrmCallRecordSaveReqVO reqVO, Long userId) {
        validateActivityObject(reqVO.getBizType(), reqVO.getBizId(), true);
        validateContact(reqVO.getBizType(), reqVO.getBizId(), reqVO.getContactId());
        if (!CrmCallDirectionEnum.contains(reqVO.getDirection())) throw exception(CALL_DIRECTION_INVALID);
        if (!CrmCallStatusEnum.contains(reqVO.getStatus())) throw exception(CALL_STATUS_INVALID);
        boolean connected = Objects.equals(reqVO.getStatus(), CrmCallStatusEnum.CONNECTED.getStatus());
        if (connected && (reqVO.getEndTime() == null || reqVO.getEndTime().isBefore(reqVO.getStartTime()))) {
            throw exception(CALL_TIME_INVALID);
        }
        if (!connected && reqVO.getEndTime() != null && reqVO.getEndTime().isBefore(reqVO.getStartTime())) {
            throw exception(CALL_TIME_INVALID);
        }
        String recordingUrl = trimToNull(reqVO.getRecordingUrl());
        if (recordingUrl != null && !recordingUrl.startsWith(properties.getProtectedCallRecordingDirectory() + "/")) {
            throw exception(CALL_RECORDING_PATH_INVALID);
        }
        long durationSeconds = connected
                ? Duration.between(reqVO.getStartTime(), reqVO.getEndTime()).getSeconds() : 0L;
        if (durationSeconds > properties.getMaxCallDurationSeconds()) {
            throw exception(CALL_TIME_INVALID);
        }
        int duration = Math.toIntExact(durationSeconds);
        CrmCallRecordDO record = BeanUtils.toBean(reqVO, CrmCallRecordDO.class)
                .setSourceClueId(null).setDurationSeconds(duration).setRecordingUrl(recordingUrl)
                .setOperatorUserId(userId);
        callRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    @CrmPermission(bizTypeValue = "#reqVO.bizType", bizId = "#reqVO.bizId",
            level = CrmPermissionLevelEnum.READ)
    public PageResult<CrmCallRecordDO> getCallRecordPage(CrmActivityPageReqVO reqVO) {
        validateActivityType(reqVO.getBizType());
        return callRecordMapper.selectPage(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizTypeValue = "#reqVO.bizType", bizId = "#reqVO.bizId",
            level = CrmPermissionLevelEnum.WRITE)
    public Long createSmsRecord(CrmSmsRecordSaveReqVO reqVO, Long userId) {
        validateActivityObject(reqVO.getBizType(), reqVO.getBizId(), true);
        validateContact(reqVO.getBizType(), reqVO.getBizId(), reqVO.getContactId());
        if (!CrmSmsDirectionEnum.contains(reqVO.getDirection())) throw exception(SMS_DIRECTION_INVALID);
        if (!CrmSmsStatusEnum.contains(reqVO.getStatus())
                || Objects.equals(reqVO.getDirection(), CrmSmsDirectionEnum.INBOUND.getDirection())
                != Objects.equals(reqVO.getStatus(), CrmSmsStatusEnum.RECEIVED.getStatus())) {
            throw exception(SMS_STATUS_INVALID);
        }
        if (Objects.equals(reqVO.getStatus(), CrmSmsStatusEnum.FAILED.getStatus())
                && StrUtil.isBlank(reqVO.getFailureReason())) throw exception(SMS_FAILURE_REASON_REQUIRED);
        CrmSmsRecordDO record = BeanUtils.toBean(reqVO, CrmSmsRecordDO.class)
                .setSourceClueId(null).setOperatorUserId(userId);
        smsRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    @CrmPermission(bizTypeValue = "#reqVO.bizType", bizId = "#reqVO.bizId",
            level = CrmPermissionLevelEnum.READ)
    public PageResult<CrmSmsRecordDO> getSmsRecordPage(CrmActivityPageReqVO reqVO) {
        validateActivityType(reqVO.getBizType());
        return smsRecordMapper.selectPage(reqVO);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CLUE, bizId = "#clueId", level = CrmPermissionLevelEnum.READ)
    public CrmClueConversionRecordDO getConversionRecord(Long clueId) {
        return conversionRecordMapper.selectByClueId(clueId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
    public CrmClueConversionRecordDO migrateClueActivities(Long clueId, Long customerId, Long primaryContactId,
                                                            int followUpCount, Long operatorUserId) {
        if (conversionRecordMapper.selectByClueId(clueId) != null) throw exception(CLUE_CONVERSION_AUDIT_EXISTS);
        List<CrmTaskDO> tasks = taskMapper.selectListByBiz(CrmBizTypeEnum.CRM_CLUE.getType(), clueId);
        int taskCount = taskMapper.migrateFromClue(clueId, customerId, operatorUserId);
        if (taskCount != tasks.size()) throw exception(CLUE_CONVERSION_AUDIT_EXISTS);
        for (CrmTaskDO task : tasks) {
            appendTaskRecord(task, CrmTaskActionTypeEnum.MIGRATE, task.getStatus(), task.getStatus(), operatorUserId,
                    "线索转客户，任务关联迁移到客户 " + customerId);
        }
        int callCount = callRecordMapper.migrateFromClue(clueId, customerId, operatorUserId);
        int smsCount = smsRecordMapper.migrateFromClue(clueId, customerId, operatorUserId);
        CrmClueConversionRecordDO record = new CrmClueConversionRecordDO().setClueId(clueId)
                .setCustomerId(customerId).setPrimaryContactId(primaryContactId)
                .setFollowUpCount(followUpCount).setTaskCount(taskCount).setCallCount(callCount).setSmsCount(smsCount)
                .setOperatorUserId(operatorUserId).setConvertedAt(LocalDateTime.now());
        try {
            conversionRecordMapper.insert(record);
        } catch (DataIntegrityViolationException ex) {
            throw exception(CLUE_CONVERSION_AUDIT_EXISTS);
        }
        return record;
    }

    @Override
    public int markOverdueTasks() {
        CrmActivityProperties.TaskOverdue policy = properties.getTaskOverdue();
        if (!policy.isEnabled()) return 0;
        LocalDateTime now = LocalDateTime.now();
        long afterId = 0L;
        int count = 0;
        for (int batch = 0; batch < policy.getMaxBatches(); batch++) {
            List<CrmTaskDO> candidates = taskMapper.selectOverdueCandidates(afterId, now,
                    policy.getBatchSize(), policy.getMaxBatchSize());
            if (CollUtil.isEmpty(candidates)) break;
            afterId = candidates.get(candidates.size() - 1).getId();
            for (CrmTaskDO task : candidates) {
                if (taskMapper.markOverdueIfOpen(task.getId(), now) == 1) {
                    appendTaskRecord(task, CrmTaskActionTypeEnum.OVERDUE, task.getStatus(),
                            CrmTaskStatusEnum.OVERDUE.getStatus(), null, "超过 YAML 调度检查时的截止时间");
                    count++;
                }
            }
            if (candidates.size() < policy.getBatchSize()) break;
        }
        return count;
    }

    private void finishTask(CrmTaskActionReqVO reqVO, Long userId, CrmTaskStatusEnum target,
                            CrmTaskActionTypeEnum action, boolean reasonRequired) {
        CrmTaskDO task = requireTaskForUpdate(reqVO.getId(), userId);
        requireAssignee(task, userId);
        String result = trimToNull(reqVO.getRemark());
        if (reasonRequired && result == null) throw exception(TASK_RESULT_REQUIRED);
        finishOpenTask(task, target, action, result == null ? "已完成" : result, userId);
    }

    private void finishOpenTask(CrmTaskDO task, CrmTaskStatusEnum target, CrmTaskActionTypeEnum action,
                                String result, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        Integer fromStatus = task.getStatus();
        if (taskMapper.finishIfOpen(task.getId(), target.getStatus(), result, userId, now) != 1) {
            throw exception(TASK_TRANSITION_INVALID);
        }
        task.setStatus(target.getStatus()).setFinishTime(now).setResult(result);
        appendTaskRecord(task, action, fromStatus, target.getStatus(), userId, result);
        notificationService.notifyFinished(task);
    }

    private CrmTaskDO requireTaskForUpdate(Long id, Long userId) {
        CrmTaskDO task = taskMapper.selectByIdForUpdate(id);
        if (task == null) throw exception(TASK_NOT_EXISTS);
        if (!permissionService.hasPermission(task.getBizType(), task.getBizId(), userId,
                CrmPermissionLevelEnum.WRITE)) throw exception(CRM_PERMISSION_DENIED, "CRM 任务");
        return task;
    }

    private void requireAssignee(CrmTaskDO task, Long userId) {
        if (!Objects.equals(task.getAssigneeUserId(), userId)) throw exception(TASK_ASSIGNEE_ONLY);
    }

    private void validateTaskFields(CrmTaskSaveReqVO reqVO, LocalDateTime now) {
        validateActivityType(reqVO.getBizType());
        if (!CrmTaskTypeEnum.contains(reqVO.getType())) throw exception(TASK_TYPE_INVALID);
        if (!CrmTaskPriorityEnum.contains(reqVO.getPriority())) throw exception(TASK_PRIORITY_INVALID);
        if (reqVO.getDueTime() == null || !reqVO.getDueTime().isAfter(now)
                || reqVO.getRemindTime() != null && reqVO.getRemindTime().isAfter(reqVO.getDueTime())) {
            throw exception(TASK_TIME_INVALID);
        }
    }

    private void validateActivityObject(Integer bizType, Long bizId, boolean forUpdate) {
        validateActivityType(bizType);
        if (Objects.equals(bizType, CrmBizTypeEnum.CRM_CLUE.getType())) {
            CrmClueDO clue = forUpdate ? clueMapper.selectByIdForUpdate(bizId) : clueMapper.selectById(bizId);
            if (clue == null) throw exception(CLUE_NOT_EXISTS);
            if (Boolean.TRUE.equals(clue.getTransformStatus())) throw exception(CLUE_UPDATE_FAIL_TRANSFORMED);
            if (!Objects.equals(clue.getPoolStatus(), CrmCluePoolStatusEnum.OWNED.getStatus())) {
                throw exception(CLUE_PUBLIC_CLAIM_REQUIRED);
            }
        } else if (customerMapper.selectById(bizId) == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
    }

    private static void validateActivityType(Integer bizType) {
        if (!Objects.equals(bizType, CrmBizTypeEnum.CRM_CLUE.getType())
                && !Objects.equals(bizType, CrmBizTypeEnum.CRM_CUSTOMER.getType())) {
            throw exception(ACTIVITY_BIZ_TYPE_INVALID);
        }
    }

    private void validateContact(Integer bizType, Long bizId, Long contactId) {
        if (contactId == null) return;
        CrmContactDO contact = contactMapper.selectById(contactId);
        if (contact == null) throw exception(CONTACT_NOT_EXISTS);
        if (!Objects.equals(bizType, CrmBizTypeEnum.CRM_CUSTOMER.getType())
                || !Objects.equals(contact.getCustomerId(), bizId)) throw exception(CONTACT_NOT_EXISTS);
    }

    private void appendTaskRecord(CrmTaskDO task, CrmTaskActionTypeEnum action, Integer fromStatus,
                                  Integer toStatus, Long userId, String remark) {
        taskActionRecordMapper.insert(new CrmTaskActionRecordDO().setTaskId(task.getId())
                .setActionType(action.getType()).setFromStatus(fromStatus).setToStatus(toStatus)
                .setOperatorUserId(userId).setRemark(remark));
    }

    private static String trimToNull(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }
}

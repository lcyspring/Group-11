package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.util.json.JsonUtils;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOATaskSaveReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOATaskDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOATaskMapper;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.notify.NotifyMessageSendApi;
import com.meession.etm.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_TASK_NOT_EXISTS;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_TASK_STATUS_INVALID;

@Service
public class BpmOATaskServiceImpl implements BpmOATaskService {
    @Resource private BpmOATaskMapper mapper;
    @Resource private AdminUserApi adminUserApi;
    @Resource private NotifyMessageSendApi notifyMessageSendApi;
    @Override public Long create(Long userId, BpmOATaskSaveReqVO req) {
        validateUsers(req);
        BpmOATaskDO task = BeanUtils.toBean(req, BpmOATaskDO.class).setCreatorUserId(userId).setStatus(0);
        mapper.insert(task); return task.getId();
    }
    @Override public void update(Long userId, BpmOATaskSaveReqVO req) {
        BpmOATaskDO old = require(userId, req.getId());
        if (!userId.equals(old.getCreatorUserId()) || old.getStatus() >= 2) throw exception(OA_TASK_STATUS_INVALID);
        validateUsers(req);
        mapper.updateById(BeanUtils.toBean(req, BpmOATaskDO.class).setId(old.getId())
                .setCreatorUserId(old.getCreatorUserId()).setStatus(old.getStatus()));
    }
    @Override public void delete(Long userId, Long id) {
        BpmOATaskDO old = require(userId, id);
        if (!userId.equals(old.getCreatorUserId()) || old.getStatus() >= 2) throw exception(OA_TASK_STATUS_INVALID);
        mapper.deleteById(id);
    }
    @Override public void start(Long userId, Long id) {
        BpmOATaskDO old = require(userId, id);
        if (!userId.equals(old.getAssigneeUserId()) || old.getStatus() != 0) throw exception(OA_TASK_STATUS_INVALID);
        mapper.updateById(new BpmOATaskDO().setId(id).setStatus(1));
    }
    @Override public void complete(Long userId, Long id, String result) {
        BpmOATaskDO old = require(userId, id);
        if (!userId.equals(old.getAssigneeUserId()) || old.getStatus() != 1) throw exception(OA_TASK_STATUS_INVALID);
        mapper.updateById(new BpmOATaskDO().setId(id).setStatus(2).setResult(result).setCompletedTime(LocalDateTime.now()));
    }
    @Override public BpmOATaskDO get(Long userId, Long id) { return require(userId, id); }
    @Override public List<BpmOATaskDO> list(Long userId, Integer status) { return mapper.selectAccessibleList(userId, status); }
    @Override public int remindDue(int limit) {
        int sent = 0; LocalDateTime now = LocalDateTime.now();
        for (BpmOATaskDO task : mapper.selectDueReminders(now, Math.max(1, Math.min(limit, 500)))) {
            if (mapper.claimReminder(task.getId()) == 0) continue;
            try {
                notifyMessageSendApi.sendSingleMessageToAdmin(new NotifySendSingleToUserReqDTO()
                        .setUserId(task.getAssigneeUserId()).setTemplateCode("bpm-oa-task-reminder")
                        .setTemplateParams(Map.of("title", task.getTitle(), "dueTime", task.getDueTime())));
                mapper.markReminderSent(task.getId(), LocalDateTime.now()); sent++;
            } catch (RuntimeException ex) {
                String error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                mapper.releaseReminder(task.getId(), error.substring(0, Math.min(error.length(), 1000)));
            }
        }
        return sent;
    }
    private BpmOATaskDO require(Long userId, Long id) {
        BpmOATaskDO task = mapper.selectById(id);
        if (task == null || !canAccess(task, userId)) throw exception(OA_TASK_NOT_EXISTS);
        return task;
    }
    private boolean canAccess(BpmOATaskDO task, Long userId) {
        if (userId.equals(task.getCreatorUserId()) || userId.equals(task.getAssigneeUserId())) return true;
        List<Long> users = task.getParticipantUserIds() == null ? Collections.emptyList()
                : JsonUtils.parseArray(task.getParticipantUserIds(), Long.class);
        return users != null && users.contains(userId);
    }
    private void validateUsers(BpmOATaskSaveReqVO req) {
        adminUserApi.validateUser(req.getAssigneeUserId());
        if (req.getParticipantUserIds() == null) return;
        List<Long> users = JsonUtils.parseArray(req.getParticipantUserIds(), Long.class);
        if (users != null && !users.isEmpty()) adminUserApi.validateUserList(users);
    }
}

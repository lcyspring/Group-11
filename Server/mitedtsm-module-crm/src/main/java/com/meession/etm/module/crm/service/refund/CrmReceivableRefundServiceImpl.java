package com.meession.etm.module.crm.service.refund;

import cn.hutool.core.util.ObjUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.common.util.object.ObjectUtils;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.crm.controller.admin.refund.vo.CrmReceivableRefundPageReqVO;
import com.meession.etm.module.crm.controller.admin.refund.vo.CrmReceivableRefundSaveReqVO;
import com.meession.etm.module.crm.controller.admin.refund.vo.CrmReceivableRefundSourceSummaryRespVO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import com.meession.etm.module.crm.dal.dataobject.refund.CrmReceivableRefundActionRecordDO;
import com.meession.etm.module.crm.dal.dataobject.refund.CrmReceivableRefundDO;
import com.meession.etm.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import com.meession.etm.module.crm.dal.mysql.refund.CrmReceivableRefundActionRecordMapper;
import com.meession.etm.module.crm.dal.mysql.refund.CrmReceivableRefundMapper;
import com.meession.etm.module.crm.dal.redis.no.CrmNoRedisDAO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.enums.refund.CrmReceivableRefundActionTypeEnum;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.framework.refund.CrmReceivableRefundProperties;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static com.meession.etm.module.crm.util.CrmAuditStatusUtils.convertBpmResultToAuditStatus;

@Service
@Validated
@Slf4j
public class CrmReceivableRefundServiceImpl implements CrmReceivableRefundService {

    private static final List<Integer> RESERVED_STATUSES = Arrays.asList(
            CrmAuditStatusEnum.PROCESS.getStatus(), CrmAuditStatusEnum.APPROVE.getStatus());

    @Resource private CrmReceivableRefundMapper refundMapper;
    @Resource private CrmReceivableRefundActionRecordMapper actionRecordMapper;
    @Resource private CrmReceivableMapper receivableMapper;
    @Resource private CrmNoRedisDAO noRedisDAO;
    @Resource private CrmPermissionService permissionService;
    @Resource private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Resource private CrmReceivableRefundProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_RECEIVABLE, bizId = "#reqVO.receivableId",
            level = CrmPermissionLevelEnum.WRITE)
    public Long createRefund(CrmReceivableRefundSaveReqVO reqVO, Long userId) {
        CrmReceivableDO receivable = validateApprovedReceivable(reqVO.getReceivableId(), false);
        validateAmount(receivable, reqVO.getAmount(), null);
        String no = noRedisDAO.generateMonthly(properties.getNumberPrefix());
        if (refundMapper.selectByNo(no) != null) {
            throw exception(RECEIVABLE_REFUND_NO_EXISTS);
        }
        CrmReceivableRefundDO refund = BeanUtils.toBean(reqVO, CrmReceivableRefundDO.class)
                .setNo(no).setCustomerId(receivable.getCustomerId()).setContractId(receivable.getContractId())
                .setOwnerUserId(receivable.getOwnerUserId()).setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus());
        refundMapper.insert(refund);
        permissionService.createPermission(new CrmPermissionCreateReqBO()
                .setBizType(CrmBizTypeEnum.CRM_RECEIVABLE_REFUND.getType()).setBizId(refund.getId())
                .setUserId(refund.getOwnerUserId()).setLevel(CrmPermissionLevelEnum.OWNER.getLevel()));
        insertAction(refund.getId(), CrmReceivableRefundActionTypeEnum.CREATE, null,
                CrmAuditStatusEnum.DRAFT.getStatus(), userId, null, reqVO.getReason());
        return refund.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_RECEIVABLE_REFUND, bizId = "#reqVO.id",
            level = CrmPermissionLevelEnum.WRITE)
    public void updateRefund(CrmReceivableRefundSaveReqVO reqVO, Long userId) {
        CrmReceivableRefundDO old = validateRefundForUpdate(reqVO.getId());
        if (!ObjectUtils.equalsAny(old.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.REJECT.getStatus(), CrmAuditStatusEnum.CANCEL.getStatus())) {
            throw exception(RECEIVABLE_REFUND_EDIT_STATUS_INVALID);
        }
        CrmReceivableDO receivable = validateApprovedReceivable(old.getReceivableId(), false);
        validateAmount(receivable, reqVO.getAmount(), old.getId());
        CrmReceivableRefundDO update = new CrmReceivableRefundDO().setId(old.getId())
                .setType(reqVO.getType()).setRefundTime(reqVO.getRefundTime()).setAmount(reqVO.getAmount())
                .setReason(reqVO.getReason()).setRemark(reqVO.getRemark());
        if (ObjUtil.notEqual(old.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus())) {
            update.setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus());
        }
        refundMapper.updateById(update);
        insertAction(old.getId(), CrmReceivableRefundActionTypeEnum.UPDATE, old.getAuditStatus(),
                update.getAuditStatus() == null ? old.getAuditStatus() : update.getAuditStatus(),
                userId, old.getProcessInstanceId(), reqVO.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_RECEIVABLE_REFUND, bizId = "#id",
            level = CrmPermissionLevelEnum.OWNER)
    public void deleteRefund(Long id, Long userId) {
        CrmReceivableRefundDO refund = validateRefundForUpdate(id);
        if (ObjUtil.notEqual(refund.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus())
                || refund.getProcessInstanceId() != null) {
            throw exception(RECEIVABLE_REFUND_DELETE_STATUS_INVALID);
        }
        insertAction(id, CrmReceivableRefundActionTypeEnum.DELETE, refund.getAuditStatus(),
                null, userId, null, "删除新草稿");
        refundMapper.deleteById(id);
        permissionService.deletePermission(CrmBizTypeEnum.CRM_RECEIVABLE_REFUND.getType(), id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_RECEIVABLE_REFUND, bizId = "#id",
            level = CrmPermissionLevelEnum.WRITE)
    public void submitRefund(Long id, Long userId) {
        CrmReceivableRefundDO current = validateRefundExists(id);
        CrmReceivableDO receivable = validateApprovedReceivable(current.getReceivableId(), true);
        CrmReceivableRefundDO refund = validateRefundForUpdate(id);
        if (ObjUtil.notEqual(refund.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus())) {
            throw exception(RECEIVABLE_REFUND_SUBMIT_STATUS_INVALID);
        }
        validateAmount(receivable, refund.getAmount(), refund.getId());
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(properties.getProcessDefinitionKey())
                        .setBusinessKey(String.valueOf(id)));
        refundMapper.updateById(new CrmReceivableRefundDO().setId(id)
                .setProcessInstanceId(processInstanceId).setAuditStatus(CrmAuditStatusEnum.PROCESS.getStatus()));
        insertAction(id, CrmReceivableRefundActionTypeEnum.SUBMIT, CrmAuditStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.PROCESS.getStatus(), userId, processInstanceId, refund.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRefundAuditStatus(Long id, String processInstanceId, Integer bpmResult) {
        Integer auditStatus = convertBpmResultToAuditStatus(bpmResult);
        CrmReceivableRefundDO refund = validateRefundExists(id);
        if (ObjUtil.equal(refund.getProcessInstanceId(), processInstanceId)
                && ObjUtil.equal(refund.getAuditStatus(), auditStatus)) {
            return;
        }
        if (ObjUtil.notEqual(refund.getProcessInstanceId(), processInstanceId)
                || ObjUtil.notEqual(refund.getAuditStatus(), CrmAuditStatusEnum.PROCESS.getStatus())) {
            log.warn("[updateRefundAuditStatus][忽略退款/冲销({})过期或乱序事件，当前流程({})、事件流程({})]",
                    id, refund.getProcessInstanceId(), processInstanceId);
            return;
        }
        if (refundMapper.updateAuditStatusIfProcessing(id, processInstanceId, auditStatus) == 0) {
            throw exception(RECEIVABLE_REFUND_CONCURRENT_CHANGE);
        }
        CrmReceivableRefundActionTypeEnum action = auditStatus.equals(CrmAuditStatusEnum.APPROVE.getStatus())
                ? CrmReceivableRefundActionTypeEnum.APPROVE
                : auditStatus.equals(CrmAuditStatusEnum.REJECT.getStatus())
                ? CrmReceivableRefundActionTypeEnum.REJECT : CrmReceivableRefundActionTypeEnum.CANCEL;
        insertAction(id, action, CrmAuditStatusEnum.PROCESS.getStatus(), auditStatus,
                null, processInstanceId, null);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_RECEIVABLE_REFUND, bizId = "#id",
            level = CrmPermissionLevelEnum.READ)
    public CrmReceivableRefundDO getRefund(Long id) {
        return validateRefundExists(id);
    }

    @Override
    public PageResult<CrmReceivableRefundDO> getRefundPage(CrmReceivableRefundPageReqVO reqVO, Long userId) {
        return refundMapper.selectPage(reqVO, userId);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_RECEIVABLE, bizId = "#receivableId",
            level = CrmPermissionLevelEnum.READ)
    public CrmReceivableRefundSourceSummaryRespVO getSourceSummary(Long receivableId, Long excludeRefundId) {
        CrmReceivableDO receivable = validateApprovedReceivable(receivableId, false);
        BigDecimal reserved = getReservedAmount(receivableId, excludeRefundId);
        return new CrmReceivableRefundSourceSummaryRespVO().setReceivableId(receivable.getId())
                .setReceivableNo(receivable.getNo()).setReceivableAmount(receivable.getPrice())
                .setReservedRefundAmount(reserved)
                .setRemainingRefundableAmount(receivable.getPrice().subtract(reserved).max(BigDecimal.ZERO));
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_RECEIVABLE_REFUND, bizId = "#refundId",
            level = CrmPermissionLevelEnum.READ)
    public List<CrmReceivableRefundActionRecordDO> getActionRecords(Long refundId) {
        validateRefundExists(refundId);
        return actionRecordMapper.selectListByRefundId(refundId);
    }

    private CrmReceivableDO validateApprovedReceivable(Long id, boolean forUpdate) {
        CrmReceivableDO receivable = forUpdate ? receivableMapper.selectByIdForUpdate(id) : receivableMapper.selectById(id);
        if (receivable == null || ObjUtil.notEqual(receivable.getAuditStatus(), CrmAuditStatusEnum.APPROVE.getStatus())) {
            throw exception(RECEIVABLE_REFUND_SOURCE_NOT_APPROVED);
        }
        return receivable;
    }

    private void validateAmount(CrmReceivableDO receivable, BigDecimal amount, Long excludeRefundId) {
        BigDecimal remaining = receivable.getPrice().subtract(getReservedAmount(receivable.getId(), excludeRefundId));
        if (amount.compareTo(remaining) > 0) {
            throw exception(RECEIVABLE_REFUND_AMOUNT_EXCEEDS, remaining.max(BigDecimal.ZERO));
        }
    }

    private BigDecimal getReservedAmount(Long receivableId, Long excludeRefundId) {
        return refundMapper.selectListByReceivableIdAndStatuses(receivableId, RESERVED_STATUSES).stream()
                .filter(item -> !ObjUtil.equal(item.getId(), excludeRefundId))
                .map(CrmReceivableRefundDO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CrmReceivableRefundDO validateRefundExists(Long id) {
        CrmReceivableRefundDO refund = refundMapper.selectById(id);
        if (refund == null) {
            throw exception(RECEIVABLE_REFUND_NOT_EXISTS);
        }
        return refund;
    }

    private CrmReceivableRefundDO validateRefundForUpdate(Long id) {
        CrmReceivableRefundDO refund = refundMapper.selectByIdForUpdate(id);
        if (refund == null) {
            throw exception(RECEIVABLE_REFUND_NOT_EXISTS);
        }
        return refund;
    }

    private void insertAction(Long refundId, CrmReceivableRefundActionTypeEnum action, Integer fromStatus,
                              Integer toStatus, Long operatorUserId, String processInstanceId, String remark) {
        actionRecordMapper.insert(new CrmReceivableRefundActionRecordDO().setRefundId(refundId)
                .setActionType(action.getAction()).setFromStatus(fromStatus).setToStatus(toStatus)
                .setOperatorUserId(operatorUserId).setActionTime(LocalDateTime.now())
                .setProcessInstanceId(processInstanceId).setRemark(remark));
    }
}

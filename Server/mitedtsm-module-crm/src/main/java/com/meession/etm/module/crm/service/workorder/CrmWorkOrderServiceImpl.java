package com.meession.etm.module.crm.service.workorder;

import cn.hutool.core.util.ObjectUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.workorder.vo.*;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderRecordDO;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderMapper;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderRecordMapper;
import com.meession.etm.module.crm.dal.redis.no.CrmNoRedisDAO;
import com.meession.etm.module.crm.enums.workorder.*;
import com.meession.etm.module.crm.service.business.CrmBusinessService;
import com.meession.etm.module.crm.service.contract.CrmContractService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

@Service
@Validated
public class CrmWorkOrderServiceImpl implements CrmWorkOrderService {

    @Resource private CrmWorkOrderMapper workOrderMapper;
    @Resource private CrmWorkOrderRecordMapper recordMapper;
    @Resource private CrmNoRedisDAO noRedisDAO;
    @Resource private CrmCustomerService customerService;
    @Resource private CrmBusinessService businessService;
    @Resource private CrmContractService contractService;
    @Resource private AdminUserApi adminUserApi;
    @Resource private CrmWorkOrderNotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkOrder(CrmWorkOrderSaveReqVO reqVO, Long userId) {
        validateRelations(reqVO.getCustomerId(), reqVO.getSourceType(), reqVO.getSourceId());
        adminUserApi.validateUser(reqVO.getHandlerUserId());
        String no = noRedisDAO.generateMonthly(CrmNoRedisDAO.WORK_ORDER_PREFIX);
        if (workOrderMapper.selectByNo(no) != null) {
            throw exception(WORK_ORDER_NO_EXISTS);
        }
        CrmWorkOrderDO workOrder = BeanUtils.toBean(reqVO, CrmWorkOrderDO.class)
                .setNo(no).setStatus(CrmWorkOrderStatusEnum.PENDING.getStatus());
        workOrder.setCreator(String.valueOf(userId));
        workOrderMapper.insert(workOrder);
        appendRecord(workOrder, CrmWorkOrderActionTypeEnum.CREATE, null,
                CrmWorkOrderStatusEnum.PENDING.getStatus(), userId, null);
        notificationService.notifyAssigned(workOrder);
        return workOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkOrder(CrmWorkOrderSaveReqVO reqVO, Long userId) {
        CrmWorkOrderDO old = requireParticipant(reqVO.getId(), userId, false);
        if (!ObjectUtil.equal(old.getCreator(), String.valueOf(userId))
                || !ObjectUtil.equal(old.getStatus(), CrmWorkOrderStatusEnum.PENDING.getStatus())
                && !ObjectUtil.equal(old.getStatus(), CrmWorkOrderStatusEnum.RETURNED.getStatus())) {
            throw exception(WORK_ORDER_CREATOR_ONLY);
        }
        // 来源、客户、处理人属于闭环主键，编辑阶段不可偷偷替换。
        validateRelations(old.getCustomerId(), old.getSourceType(), old.getSourceId());
        CrmWorkOrderDO update = BeanUtils.toBean(reqVO, CrmWorkOrderDO.class)
                .setId(old.getId()).setCustomerId(old.getCustomerId()).setSourceType(old.getSourceType())
                .setSourceId(old.getSourceId()).setHandlerUserId(old.getHandlerUserId())
                .setStatus(old.getStatus());
        workOrderMapper.updateById(update);
        appendRecord(old, CrmWorkOrderActionTypeEnum.UPDATE, old.getStatus(), old.getStatus(), userId,
                "修改工单内容");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkOrder(Long id, Long userId) {
        CrmWorkOrderDO old = requireParticipant(id, userId, false);
        if (!ObjectUtil.equal(old.getCreator(), String.valueOf(userId))
                || !ObjectUtil.equal(old.getStatus(), CrmWorkOrderStatusEnum.PENDING.getStatus())) {
            throw exception(WORK_ORDER_DELETE_STATUS_INVALID);
        }
        recordMapper.deleteByWorkOrderId(id);
        workOrderMapper.deleteById(id);
    }

    @Override
    public CrmWorkOrderDO getWorkOrder(Long id, Long userId, boolean queryAll) {
        return requireParticipant(id, userId, queryAll);
    }

    @Override
    public PageResult<CrmWorkOrderDO> getWorkOrderPage(CrmWorkOrderPageReqVO reqVO, Long userId, boolean queryAll) {
        return workOrderMapper.selectPage(reqVO, userId, queryAll);
    }

    @Override
    public List<CrmWorkOrderRecordDO> getWorkOrderRecords(Long id, Long userId, boolean queryAll) {
        requireParticipant(id, userId, queryAll);
        return recordMapper.selectListByWorkOrderId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startWorkOrder(CrmWorkOrderActionReqVO reqVO, Long userId) {
        CrmWorkOrderDO old = requireParticipant(reqVO.getId(), userId, false);
        requireHandler(old, userId);
        transition(old, workOrderMapper.startIfPending(old.getId(), CrmWorkOrderStatusEnum.PENDING.getStatus(),
                CrmWorkOrderStatusEnum.PROCESSING.getStatus(), LocalDateTime.now()),
                CrmWorkOrderActionTypeEnum.START, userId, reqVO.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnWorkOrder(CrmWorkOrderReturnReqVO reqVO, Long userId) {
        CrmWorkOrderDO old = requireParticipant(reqVO.getId(), userId, false);
        requireHandler(old, userId);
        transition(old, workOrderMapper.returnIfProcessing(old.getId(), CrmWorkOrderStatusEnum.PROCESSING.getStatus(),
                CrmWorkOrderStatusEnum.RETURNED.getStatus(), reqVO.getReason()),
                CrmWorkOrderActionTypeEnum.RETURN, userId, reqVO.getReason());
        old.setStatus(CrmWorkOrderStatusEnum.RETURNED.getStatus()).setReturnReason(reqVO.getReason());
        notificationService.notifyReturned(old);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmitWorkOrder(CrmWorkOrderActionReqVO reqVO, Long userId) {
        CrmWorkOrderDO old = requireParticipant(reqVO.getId(), userId, false);
        if (!ObjectUtil.equal(old.getCreator(), String.valueOf(userId))) {
            throw exception(WORK_ORDER_CREATOR_ONLY);
        }
        transition(old, workOrderMapper.resubmitIfReturned(old.getId(), CrmWorkOrderStatusEnum.RETURNED.getStatus(),
                CrmWorkOrderStatusEnum.PENDING.getStatus()),
                CrmWorkOrderActionTypeEnum.RESUBMIT, userId, reqVO.getRemark());
        old.setStatus(CrmWorkOrderStatusEnum.PENDING.getStatus());
        notificationService.notifyAssigned(old);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeWorkOrder(CrmWorkOrderCompleteReqVO reqVO, Long userId) {
        CrmWorkOrderDO old = requireParticipant(reqVO.getId(), userId, false);
        requireHandler(old, userId);
        if (reqVO.getSolution() == null || reqVO.getSolution().isBlank()) {
            throw exception(WORK_ORDER_SOLUTION_REQUIRED);
        }
        transition(old, workOrderMapper.completeIfProcessing(old.getId(), CrmWorkOrderStatusEnum.PROCESSING.getStatus(),
                CrmWorkOrderStatusEnum.COMPLETED.getStatus(), reqVO.getSolution(), LocalDateTime.now()),
                CrmWorkOrderActionTypeEnum.COMPLETE, userId, null);
        old.setStatus(CrmWorkOrderStatusEnum.COMPLETED.getStatus()).setSolution(reqVO.getSolution());
        notificationService.notifyCompleted(old);
    }

    private void validateRelations(Long customerId, Integer sourceType, Long sourceId) {
        customerService.validateCustomer(customerId);
        if (CrmWorkOrderSourceTypeEnum.CUSTOMER.getType().equals(sourceType)) {
            if (sourceId != null) throw exception(WORK_ORDER_SOURCE_CUSTOMER_MISMATCH);
            return;
        }
        if (sourceId == null) throw exception(WORK_ORDER_SOURCE_CUSTOMER_MISMATCH);
        Long sourceCustomerId;
        if (CrmWorkOrderSourceTypeEnum.BUSINESS.getType().equals(sourceType)) {
            CrmBusinessDO business = businessService.validateBusiness(sourceId);
            sourceCustomerId = business.getCustomerId();
        } else if (CrmWorkOrderSourceTypeEnum.CONTRACT.getType().equals(sourceType)) {
            CrmContractDO contract = contractService.validateContract(sourceId);
            sourceCustomerId = contract.getCustomerId();
        } else {
            throw exception(WORK_ORDER_SOURCE_CUSTOMER_MISMATCH);
        }
        if (!ObjectUtil.equal(customerId, sourceCustomerId)) {
            throw exception(WORK_ORDER_SOURCE_CUSTOMER_MISMATCH);
        }
    }

    private CrmWorkOrderDO requireParticipant(Long id, Long userId, boolean queryAll) {
        CrmWorkOrderDO workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) throw exception(WORK_ORDER_NOT_EXISTS);
        if (!queryAll && !ObjectUtil.equal(workOrder.getCreator(), String.valueOf(userId))
                && !ObjectUtil.equal(workOrder.getHandlerUserId(), userId)) {
            throw exception(WORK_ORDER_QUERY_DENIED);
        }
        return workOrder;
    }

    private void requireHandler(CrmWorkOrderDO workOrder, Long userId) {
        if (!ObjectUtil.equal(workOrder.getHandlerUserId(), userId)) throw exception(WORK_ORDER_HANDLER_ONLY);
    }

    private void transition(CrmWorkOrderDO old, int updated, CrmWorkOrderActionTypeEnum action,
                            Long userId, String remark) {
        if (updated != 1) throw exception(WORK_ORDER_STATUS_TRANSITION_INVALID);
        Integer toStatus = switch (action) {
            case START -> CrmWorkOrderStatusEnum.PROCESSING.getStatus();
            case RETURN -> CrmWorkOrderStatusEnum.RETURNED.getStatus();
            case RESUBMIT -> CrmWorkOrderStatusEnum.PENDING.getStatus();
            case COMPLETE -> CrmWorkOrderStatusEnum.COMPLETED.getStatus();
            default -> old.getStatus();
        };
        appendRecord(old, action, old.getStatus(), toStatus, userId, remark);
    }

    private void appendRecord(CrmWorkOrderDO order, CrmWorkOrderActionTypeEnum action, Integer from, Integer to,
                              Long userId, String remark) {
        CrmWorkOrderRecordDO record = new CrmWorkOrderRecordDO().setWorkOrderId(order.getId())
                .setActionType(action.getType()).setFromStatus(from).setToStatus(to)
                .setOperatorUserId(userId).setHandlerUserId(order.getHandlerUserId()).setRemark(remark);
        recordMapper.insert(record);
    }
}

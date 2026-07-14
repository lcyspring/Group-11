package com.meession.etm.module.crm.service.receivable;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.receivable.vo.plan.CrmReceivablePlanPageReqVO;
import com.meession.etm.module.crm.controller.admin.receivable.vo.plan.CrmReceivablePlanSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivablePlanDO;
import com.meession.etm.module.crm.dal.mysql.receivable.CrmReceivablePlanMapper;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.service.contract.CrmContractService;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.RECEIVABLE_PLAN_CREATE_FAIL_CONTRACT_NOT_APPROVE;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.RECEIVABLE_PLAN_DELETE_FAIL_LINKED;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.RECEIVABLE_PLAN_EXISTS_RECEIVABLE;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.RECEIVABLE_PLAN_NOT_EXISTS;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.RECEIVABLE_PLAN_PRICE_EXCEEDS_CONTRACT;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.RECEIVABLE_PLAN_UPDATE_FAIL;
import static com.meession.etm.module.crm.enums.LogRecordConstants.*;

/**
 * 回款计划 Service 实现类
 *
 * @author 密讯
 */
@Service
@Validated
public class CrmReceivablePlanServiceImpl implements CrmReceivablePlanService {

    @Resource
    private CrmReceivablePlanMapper receivablePlanMapper;

    @Resource
    private CrmContractService contractService;
    @Resource
    private CrmPermissionService permissionService;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_RECEIVABLE_PLAN_TYPE, subType = CRM_RECEIVABLE_PLAN_CREATE_SUB_TYPE, bizNo = "{{#receivablePlan.id}}",
            success = CRM_RECEIVABLE_PLAN_CREATE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#createReqVO.contractId",
            level = CrmPermissionLevelEnum.WRITE)
    public Long createReceivablePlan(CrmReceivablePlanSaveReqVO createReqVO) {
        // 1. 锁定合同后校验关系和计划总额，串行化期次与金额分配
        validateOwnerUser(createReqVO.getOwnerUserId());
        receivablePlanMapper.selectContractIdForUpdate(createReqVO.getContractId());
        CrmContractDO contract = validateAndFillContract(createReqVO);
        validateTotalPrice(createReqVO.getPrice(), contract, null);

        // 2. 插入回款计划
        CrmReceivablePlanDO maxPeriodReceivablePlan = receivablePlanMapper.selectMaxPeriodByContractId(createReqVO.getContractId());
        int period = maxPeriodReceivablePlan == null ? 1 : maxPeriodReceivablePlan.getPeriod() + 1;
        CrmReceivablePlanDO receivablePlan = BeanUtils.toBean(createReqVO, CrmReceivablePlanDO.class).setPeriod(period);
        if (createReqVO.getReturnTime() != null && createReqVO.getRemindDays() != null) {
            receivablePlan.setRemindTime(createReqVO.getReturnTime().minusDays(createReqVO.getRemindDays()));
        }
        receivablePlanMapper.insert(receivablePlan);

        // 3. 创建数据权限
        permissionService.createPermission(new CrmPermissionCreateReqBO().setUserId(createReqVO.getOwnerUserId())
                .setBizType(CrmBizTypeEnum.CRM_RECEIVABLE_PLAN.getType()).setBizId(receivablePlan.getId())
                .setLevel(CrmPermissionLevelEnum.OWNER.getLevel()));

        // 4. 记录操作日志上下文
        LogRecordContext.putVariable("receivablePlan", receivablePlan);
        return receivablePlan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_RECEIVABLE_PLAN_TYPE, subType = CRM_RECEIVABLE_PLAN_UPDATE_SUB_TYPE, bizNo = "{{#updateReqVO.id}}",
            success = CRM_RECEIVABLE_PLAN_UPDATE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_RECEIVABLE_PLAN, bizId = "#updateReqVO.id", level = CrmPermissionLevelEnum.WRITE)
    public void updateReceivablePlan(CrmReceivablePlanSaveReqVO updateReqVO) {
        updateReqVO.setOwnerUserId(null).setCustomerId(null).setContractId(null); // 防止修改这些字段
        // 1.1 先读取合同编号，再按合同、计划顺序加锁
        CrmReceivablePlanDO oldReceivablePlan = validateReceivablePlanExists(updateReqVO.getId());
        receivablePlanMapper.selectContractIdForUpdate(oldReceivablePlan.getContractId());
        oldReceivablePlan = validateReceivablePlanExistsForUpdate(updateReqVO.getId());
        // 1.2 如果已经有对应的回款，则不允许编辑
        if (Objects.nonNull(oldReceivablePlan.getReceivableId())) {
            throw exception(RECEIVABLE_PLAN_UPDATE_FAIL);
        }
        // 1.3 只允许修改可变字段，并校验更新后的合同计划总额
        updateReqVO.setOwnerUserId(oldReceivablePlan.getOwnerUserId())
                .setCustomerId(oldReceivablePlan.getCustomerId())
                .setContractId(oldReceivablePlan.getContractId());
        CrmContractDO contract = validateAndFillContract(updateReqVO);
        validateTotalPrice(updateReqVO.getPrice(), contract, oldReceivablePlan.getId());

        // 2. 更新回款计划
        CrmReceivablePlanDO updateObj = BeanUtils.toBean(updateReqVO, CrmReceivablePlanDO.class);
        if (updateReqVO.getReturnTime() != null && updateReqVO.getRemindDays() != null) {
            updateObj.setRemindTime(updateReqVO.getReturnTime().minusDays(updateReqVO.getRemindDays()));
        }
        receivablePlanMapper.updateById(updateObj);

        // 3. 记录操作日志上下文
        updateReqVO.setOwnerUserId(oldReceivablePlan.getOwnerUserId()); // 避免操作日志出现“删除负责人”的情况
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtils.toBean(oldReceivablePlan, CrmReceivablePlanSaveReqVO.class));
        LogRecordContext.putVariable("receivablePlan", oldReceivablePlan);
    }

    private void validateOwnerUser(Long ownerUserId) {
        if (ownerUserId != null) {
            adminUserApi.validateUser(ownerUserId);
        }
    }

    private CrmContractDO validateAndFillContract(CrmReceivablePlanSaveReqVO reqVO) {
        CrmContractDO contract = contractService.validateContract(reqVO.getContractId());
        if (!CrmAuditStatusEnum.APPROVE.getStatus().equals(contract.getAuditStatus())) {
            throw exception(RECEIVABLE_PLAN_CREATE_FAIL_CONTRACT_NOT_APPROVE);
        }
        reqVO.setCustomerId(contract.getCustomerId());
        return contract;
    }

    private void validateTotalPrice(BigDecimal price, CrmContractDO contract, Long excludedPlanId) {
        List<CrmReceivablePlanDO> plans = receivablePlanMapper.selectListByContractId(contract.getId());
        BigDecimal plannedPrice = plans.stream()
                .filter(plan -> !Objects.equals(plan.getId(), excludedPlanId))
                .map(CrmReceivablePlanDO::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingPrice = contract.getTotalPrice().subtract(plannedPrice);
        if (price.compareTo(remainingPrice) > 0) {
            throw exception(RECEIVABLE_PLAN_PRICE_EXCEEDS_CONTRACT, remainingPrice.max(BigDecimal.ZERO));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReceivablePlanReceivableId(Long id, Long receivableId) {
        // 锁定计划，防止并发创建多个回款或与删除串写
        CrmReceivablePlanDO receivablePlan = validateReceivablePlanExistsForUpdate(id);
        if (receivablePlan.getReceivableId() != null) {
            throw exception(RECEIVABLE_PLAN_EXISTS_RECEIVABLE);
        }
        // 更新回款计划
        receivablePlanMapper.updateById(new CrmReceivablePlanDO().setId(id).setReceivableId(receivableId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_RECEIVABLE_PLAN_TYPE, subType = CRM_RECEIVABLE_PLAN_DELETE_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_RECEIVABLE_PLAN_DELETE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_RECEIVABLE_PLAN, bizId = "#id", level = CrmPermissionLevelEnum.OWNER)
    public void deleteReceivablePlan(Long id) {
        // 1. 锁定并校验存在，已关联回款的计划必须保留业务关系
        CrmReceivablePlanDO receivablePlan = validateReceivablePlanExistsForUpdate(id);
        if (receivablePlan.getReceivableId() != null) {
            throw exception(RECEIVABLE_PLAN_DELETE_FAIL_LINKED);
        }

        // 2. 删除
        receivablePlanMapper.deleteById(id);
        // 3. 删除数据权限
        permissionService.deletePermission(CrmBizTypeEnum.CRM_RECEIVABLE_PLAN.getType(), id);

        // 4. 记录操作日志上下文
        LogRecordContext.putVariable("receivablePlan", receivablePlan);
    }

    private CrmReceivablePlanDO validateReceivablePlanExists(Long id) {
        CrmReceivablePlanDO receivablePlan = receivablePlanMapper.selectById(id);
        if (receivablePlan == null) {
            throw exception(RECEIVABLE_PLAN_NOT_EXISTS);
        }
        return receivablePlan;
    }

    private CrmReceivablePlanDO validateReceivablePlanExistsForUpdate(Long id) {
        CrmReceivablePlanDO receivablePlan = receivablePlanMapper.selectByIdForUpdate(id);
        if (receivablePlan == null) {
            throw exception(RECEIVABLE_PLAN_NOT_EXISTS);
        }
        return receivablePlan;
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_RECEIVABLE_PLAN, bizId = "#id", level = CrmPermissionLevelEnum.READ)
    public CrmReceivablePlanDO getReceivablePlan(Long id) {
        return receivablePlanMapper.selectById(id);
    }

    @Override
    public List<CrmReceivablePlanDO> getReceivablePlanList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return ListUtil.empty();
        }
        return receivablePlanMapper.selectByIds(ids);
    }

    @Override
    public PageResult<CrmReceivablePlanDO> getReceivablePlanPage(CrmReceivablePlanPageReqVO pageReqVO, Long userId) {
        return receivablePlanMapper.selectPage(pageReqVO, userId);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#pageReqVO.customerId", level = CrmPermissionLevelEnum.READ)
    public PageResult<CrmReceivablePlanDO> getReceivablePlanPageByCustomerId(CrmReceivablePlanPageReqVO pageReqVO) {
        return receivablePlanMapper.selectPageByCustomerId(pageReqVO);
    }

    @Override
    public Long getReceivablePlanRemindCount(Long userId) {
        return receivablePlanMapper.selectReceivablePlanCountByRemind(userId);
    }

}

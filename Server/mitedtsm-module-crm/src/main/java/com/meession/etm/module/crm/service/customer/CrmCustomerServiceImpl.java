package com.meession.etm.module.crm.service.customer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.collection.CollectionUtils;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.business.vo.business.CrmBusinessTransferReqVO;
import com.meession.etm.module.crm.controller.admin.contact.vo.CrmContactTransferReqVO;
import com.meession.etm.module.crm.controller.admin.contract.vo.contract.CrmContractTransferReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.*;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerLimitConfigDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerLifecycleRecordDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerOwnerRecordDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolConfigDO;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerLifecycleRecordMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerOwnerRecordMapper;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.common.CrmSceneTypeEnum;
import com.meession.etm.module.crm.enums.customer.CrmCustomerOwnerRecordTypeEnum;
import com.meession.etm.module.crm.enums.customer.CrmCustomerLifecycleStatusEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.service.business.CrmBusinessService;
import com.meession.etm.module.crm.service.contact.CrmContactService;
import com.meession.etm.module.crm.service.contract.CrmContractService;
import com.meession.etm.module.crm.service.customer.bo.CrmCustomerCreateReqBO;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
import com.meession.etm.module.crm.service.permission.bo.CrmPermissionTransferReqBO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.*;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.filterList;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static com.meession.etm.module.crm.enums.LogRecordConstants.*;
import static com.meession.etm.module.crm.enums.customer.CrmCustomerLimitConfigTypeEnum.CUSTOMER_LOCK_LIMIT;
import static com.meession.etm.module.crm.enums.customer.CrmCustomerLimitConfigTypeEnum.CUSTOMER_OWNER_LIMIT;
import static com.meession.etm.module.crm.enums.customer.CrmCustomerOwnerRecordTypeEnum.INITIAL_ASSIGN;
import static com.meession.etm.module.crm.enums.customer.CrmCustomerOwnerRecordTypeEnum.PUT_POOL;
import static com.meession.etm.module.crm.enums.customer.CrmCustomerOwnerRecordTypeEnum.TAKE_POOL;
import static com.meession.etm.module.crm.enums.customer.CrmCustomerOwnerRecordTypeEnum.TRANSFER;
import static java.util.Collections.singletonList;

/**
 * 客户 Service 实现类
 *
 * @author Wanwan
 */
@Service
@Slf4j
@Validated
public class CrmCustomerServiceImpl implements CrmCustomerService {

    @Resource
    private CrmCustomerMapper customerMapper;
    @Resource
    private CrmCustomerOwnerRecordMapper customerOwnerRecordMapper;
    @Resource
    private CrmCustomerLifecycleRecordMapper customerLifecycleRecordMapper;

    @Resource
    private CrmPermissionService permissionService;
    @Resource
    private CrmCustomerLimitConfigService customerLimitConfigService;
    @Resource
    @Lazy
    private CrmCustomerPoolConfigService customerPoolConfigService;
    @Resource
    @Lazy
    private CrmContactService contactService;
    @Resource
    @Lazy
    private CrmBusinessService businessService;
    @Resource
    @Lazy
    private CrmContractService contractService;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_CREATE_SUB_TYPE, bizNo = "{{#customer.id}}",
            success = CRM_CUSTOMER_CREATE_SUCCESS)
    public Long createCustomer(CrmCustomerSaveReqVO createReqVO, Long userId) {
        createReqVO.setId(null);
        // 1.1 校验客户层级。存在父客户时先锁定租户层级，和更新、删除操作串行化
        if (createReqVO.getParentCustomerId() != null) {
            validateCustomerHierarchy(null, createReqVO.getParentCustomerId(),
                    customerMapper.selectHierarchyListForUpdate());
        }
        // 1.2 校验客户名称唯一
        validateCustomerNameUnique(null, createReqVO.getName());
        // 1.3 校验所选负责人存在且拥有客户未达到上限
        Long ownerUserId = createReqVO.getOwnerUserId();
        adminUserApi.validateUser(ownerUserId);
        validateCustomerExceedOwnerLimit(ownerUserId, 1);

        // 2. 插入客户
        CrmCustomerDO customer = initCustomer(createReqVO, ownerUserId);
        customerMapper.insert(customer);

        // 3. 创建数据权限
        permissionService.createPermission(new CrmPermissionCreateReqBO().setBizType(CrmBizTypeEnum.CRM_CUSTOMER.getType())
                .setBizId(customer.getId()).setUserId(ownerUserId).setLevel(CrmPermissionLevelEnum.OWNER.getLevel()));

        // 4. 记录初始归属，与客户和数据权限在同一事务内提交
        createOwnerRecord(customer.getId(), null, ownerUserId, INITIAL_ASSIGN);

        // 5. 记录操作日志上下文
        LogRecordContext.putVariable("customer", customer);
        LogRecordContext.putVariable("duplicateCheckDecision", Boolean.TRUE.equals(createReqVO.getDuplicateCheckConfirmed())
                ? "（已确认疑似重复客户后继续创建）" : "");
        return customer.getId();
    }

    /**
     * 初始化客户的通用字段
     *
     * @param customer    客户信息
     * @param ownerUserId 负责人编号
     * @return 客户信息 DO
     */
    private static CrmCustomerDO initCustomer(Object customer, Long ownerUserId) {
        return BeanUtils.toBean(customer, CrmCustomerDO.class).setOwnerUserId(ownerUserId)
                .setOwnerTime(LocalDateTime.now())
                .setLifecycleStatus(CrmCustomerLifecycleStatusEnum.POTENTIAL.getStatus())
                .setLifecycleStatusChangeTime(LocalDateTime.now())
                .setLifecycleLostReason(null)
                .setDealStatus(false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_UPDATE_SUB_TYPE, bizNo = "{{#updateReqVO.id}}",
            success = CRM_CUSTOMER_UPDATE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#updateReqVO.id", level = CrmPermissionLevelEnum.WRITE)
    public void updateCustomer(CrmCustomerSaveReqVO updateReqVO) {
        Assert.notNull(updateReqVO.getId(), "客户编号不能为空");
        updateReqVO.setOwnerUserId(null);  // 更新的时候，要把 updateReqVO 负责人设置为空，避免修改
        // 1.1 锁定并校验当前租户的客户层级，防止并发更新分别通过校验后形成环
        List<CrmCustomerDO> hierarchy = customerMapper.selectHierarchyListForUpdate();
        validateCustomerHierarchy(updateReqVO.getId(), updateReqVO.getParentCustomerId(), hierarchy);
        // 1.2 校验存在及名称唯一
        CrmCustomerDO oldCustomer = validateCustomerExists(updateReqVO.getId());
        validateCustomerNameUnique(updateReqVO.getId(), updateReqVO.getName());

        // 2. 更新客户
        CrmCustomerDO updateObj = BeanUtils.toBean(updateReqVO, CrmCustomerDO.class);
        customerMapper.updateById(updateObj);
        // MyBatis 默认忽略 null 字段，因此父关系必须显式更新，才能支持解除上下级关系
        customerMapper.updateParentCustomerIdById(updateReqVO.getId(), updateReqVO.getParentCustomerId());

        // 3. 记录操作日志上下文
        updateReqVO.setOwnerUserId(oldCustomer.getOwnerUserId()); // 避免操作日志出现“删除负责人”的情况
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtils.toBean(oldCustomer, CrmCustomerSaveReqVO.class));
        LogRecordContext.putVariable("customerName", oldCustomer.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_UPDATE_DEAL_STATUS_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_CUSTOMER_UPDATE_DEAL_STATUS_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#id", level = CrmPermissionLevelEnum.WRITE)
    public void updateCustomerDealStatus(Long id, Boolean dealStatus, Long operatorUserId) {
        CrmCustomerLifecycleUpdateReqVO reqVO = new CrmCustomerLifecycleUpdateReqVO().setId(id)
                .setLifecycleStatus(Boolean.TRUE.equals(dealStatus)
                        ? CrmCustomerLifecycleStatusEnum.DEAL.getStatus()
                        : CrmCustomerLifecycleStatusEnum.POTENTIAL.getStatus())
                .setReason("兼容成交状态接口变更");
        updateCustomerLifecycleStatus(reqVO, operatorUserId);
        LogRecordContext.putVariable("dealStatus", dealStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_UPDATE_LIFECYCLE_STATUS_SUB_TYPE,
            bizNo = "{{#reqVO.id}}", success = CRM_CUSTOMER_UPDATE_LIFECYCLE_STATUS_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#reqVO.id", level = CrmPermissionLevelEnum.WRITE)
    public void updateCustomerLifecycleStatus(CrmCustomerLifecycleUpdateReqVO reqVO, Long operatorUserId) {
        CrmCustomerDO customer = customerMapper.selectByIdForUpdate(reqVO.getId());
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        Integer fromStatus = normalizeLifecycleStatus(customer);
        Integer toStatus = reqVO.getLifecycleStatus();
        if (!CrmCustomerLifecycleStatusEnum.isValid(toStatus)) {
            throw exception(CUSTOMER_LIFECYCLE_STATUS_INVALID, toStatus);
        }
        if (Objects.equals(fromStatus, toStatus)) {
            throw exception(CUSTOMER_LIFECYCLE_STATUS_SAME);
        }
        String reason = StrUtil.trim(reqVO.getReason());
        if (CrmCustomerLifecycleStatusEnum.isLost(toStatus) && StrUtil.isBlank(reason)) {
            throw exception(CUSTOMER_LIFECYCLE_LOST_REASON_REQUIRED);
        }

        LocalDateTime changeTime = LocalDateTime.now();
        customerMapper.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CrmCustomerDO>()
                .eq(CrmCustomerDO::getId, customer.getId())
                .set(CrmCustomerDO::getLifecycleStatus, toStatus)
                .set(CrmCustomerDO::getLifecycleStatusChangeTime, changeTime)
                .set(CrmCustomerDO::getLifecycleLostReason,
                        CrmCustomerLifecycleStatusEnum.isLost(toStatus) ? reason : null)
                .set(CrmCustomerDO::getDealStatus, CrmCustomerLifecycleStatusEnum.isDeal(toStatus)));
        customerLifecycleRecordMapper.insert(new CrmCustomerLifecycleRecordDO()
                .setCustomerId(customer.getId()).setFromStatus(fromStatus).setToStatus(toStatus)
                .setReason(reason).setOperatorUserId(operatorUserId).setChangeTime(changeTime));

        LogRecordContext.putVariable("customerName", customer.getName());
        LogRecordContext.putVariable("lifecycleStatusName",
                CrmCustomerLifecycleStatusEnum.getNameByStatus(toStatus));
    }

    private static Integer normalizeLifecycleStatus(CrmCustomerDO customer) {
        if (customer.getLifecycleStatus() != null) {
            return customer.getLifecycleStatus();
        }
        return Boolean.TRUE.equals(customer.getDealStatus())
                ? CrmCustomerLifecycleStatusEnum.DEAL.getStatus()
                : CrmCustomerLifecycleStatusEnum.POTENTIAL.getStatus();
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#customerId", level = CrmPermissionLevelEnum.READ)
    public List<CrmCustomerLifecycleRecordDO> getCustomerLifecycleRecordList(Long customerId) {
        validateCustomerExists(customerId);
        return customerLifecycleRecordMapper.selectListByCustomerId(customerId);
    }

    @Override
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_FOLLOW_UP_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_CUSTOMER_FOLLOW_UP_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#id", level = CrmPermissionLevelEnum.WRITE)
    public void updateCustomerFollowUp(Long id, LocalDateTime contactNextTime, String contactLastContent) {
        // 1.1 校验存在
        CrmCustomerDO customer = validateCustomerExists(id);

        // 2. 更新客户的跟进信息
        customerMapper.updateById(new CrmCustomerDO().setId(id).setFollowUpStatus(true).setContactNextTime(contactNextTime)
                .setContactLastTime(LocalDateTime.now()).setContactLastContent(contactLastContent));

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("customerName", customer.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_DELETE_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_CUSTOMER_DELETE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#id", level = CrmPermissionLevelEnum.OWNER)
    public void deleteCustomer(Long id) {
        // 1.1 锁定层级，避免删除与建立下级关系并发造成孤儿关系
        List<CrmCustomerDO> hierarchy = customerMapper.selectHierarchyListForUpdate();
        // 1.2 校验存在
        CrmCustomerDO customer = validateCustomerExists(id);
        // 1.3 检查引用
        validateCustomerReference(id, hierarchy);

        // 2. 删除客户
        customerMapper.deleteById(id);
        // 3. 删除数据权限
        permissionService.deletePermission(CrmBizTypeEnum.CRM_CUSTOMER.getType(), id);

        // 4. 记录操作日志上下文
        LogRecordContext.putVariable("customerName", customer.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_TRANSFER_SUB_TYPE, bizNo = "{{#reqVO.id}}",
            success = CRM_CUSTOMER_TRANSFER_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#reqVO.id", level = CrmPermissionLevelEnum.OWNER)
    public void transferCustomer(CrmCustomerTransferReqVO reqVO, Long userId) {
        // 1.1 校验客户是否存在
        CrmCustomerDO customer = validateCustomerExists(reqVO.getId());
        // 1.2 校验拥有客户是否到达上限
        validateCustomerExceedOwnerLimit(reqVO.getNewOwnerUserId(), 1);
        // 2.1 数据权限转移
        permissionService.transferPermission(new CrmPermissionTransferReqBO(userId, CrmBizTypeEnum.CRM_CUSTOMER.getType(),
                reqVO.getId(), reqVO.getNewOwnerUserId(), reqVO.getOldOwnerPermissionLevel()));
        // 2.2 转移后重新设置负责人
        customerMapper.updateById(new CrmCustomerDO().setId(reqVO.getId())
                .setOwnerUserId(reqVO.getNewOwnerUserId()).setOwnerTime(LocalDateTime.now()));

        // 2.3 同时转移
        if (CollUtil.isNotEmpty(reqVO.getToBizTypes())) {
            transfer(reqVO, userId);
        }

        // 3. 记录转移前后归属。位于关联对象转移之后，任一步失败均整体回滚
        createOwnerRecord(customer.getId(), customer.getOwnerUserId(), reqVO.getNewOwnerUserId(), TRANSFER);

        // 4. 记录转移日志
        LogRecordContext.putVariable("customer", customer);
    }

    /**
     * 转移客户时，需要额外有【联系人】【商机】【合同】
     *
     * @param reqVO  请求
     * @param userId 用户编号
     */
    private void transfer(CrmCustomerTransferReqVO reqVO, Long userId) {
        if (reqVO.getToBizTypes().contains(CrmBizTypeEnum.CRM_CONTACT.getType())) {
            List<CrmContactDO> contactList = contactService.getContactListByCustomerIdOwnerUserId(reqVO.getId(), userId);
            contactList.forEach(item -> contactService.transferContact(new CrmContactTransferReqVO(item.getId(), reqVO.getNewOwnerUserId(),
                    reqVO.getOldOwnerPermissionLevel()), userId));
        }
        if (reqVO.getToBizTypes().contains(CrmBizTypeEnum.CRM_BUSINESS.getType())) {
            List<CrmBusinessDO> businessList = businessService.getBusinessListByCustomerIdOwnerUserId(reqVO.getId(), userId);
            businessList.forEach(item -> businessService.transferBusiness(new CrmBusinessTransferReqVO(item.getId(), reqVO.getNewOwnerUserId(),
                    reqVO.getOldOwnerPermissionLevel()), userId));
        }
        if (reqVO.getToBizTypes().contains(CrmBizTypeEnum.CRM_CONTRACT.getType())) {
            List<CrmContractDO> contractList = contractService.getContractListByCustomerIdOwnerUserId(reqVO.getId(), userId);
            contractList.forEach(item -> contractService.transferContract(new CrmContractTransferReqVO(item.getId(), reqVO.getNewOwnerUserId(),
                    reqVO.getOldOwnerPermissionLevel()), userId));
        }
    }

    @Override
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_LOCK_SUB_TYPE, bizNo = "{{#lockReqVO.id}}",
            success = CRM_CUSTOMER_LOCK_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#lockReqVO.id", level = CrmPermissionLevelEnum.OWNER)
    public void lockCustomer(CrmCustomerLockReqVO lockReqVO, Long userId) {
        // 1.1 校验当前客户是否存在
        CrmCustomerDO customer = validateCustomerExists(lockReqVO.getId());
        // 1.2 校验当前是否重复操作锁定/解锁状态
        if (customer.getLockStatus().equals(lockReqVO.getLockStatus())) {
            throw exception(customer.getLockStatus() ? CUSTOMER_LOCK_FAIL_IS_LOCK : CUSTOMER_UNLOCK_FAIL_IS_UNLOCK);
        }
        // 1.3 校验锁定上限
        if (lockReqVO.getLockStatus()) {
            validateCustomerExceedLockLimit(userId);
        }

        // 2. 更新锁定状态
        customerMapper.updateById(BeanUtils.toBean(lockReqVO, CrmCustomerDO.class));

        // 3. 记录操作日志上下文
        // tips: 因为这里使用的是老的状态所以记录时反着记录，也就是 lockStatus 为 true 那么就是解锁反之为锁定
        LogRecordContext.putVariable("customer", customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_CREATE_SUB_TYPE, bizNo = "{{#customer.id}}",
            success = CRM_CUSTOMER_CREATE_SUCCESS)
    public Long createCustomer(CrmCustomerCreateReqBO createReqBO, Long userId) {
        // 1. 校验客户名称唯一，避免线索转换产生重复客户
        validateCustomerNameUnique(null, createReqBO.getName());

        // 2. 插入客户
        CrmCustomerDO customer = initCustomer(createReqBO, userId);
        customerMapper.insert(customer);

        // 3. 创建数据权限
        permissionService.createPermission(new CrmPermissionCreateReqBO().setBizType(CrmBizTypeEnum.CRM_CUSTOMER.getType())
                .setBizId(customer.getId()).setUserId(userId).setLevel(CrmPermissionLevelEnum.OWNER.getLevel())); // 设置当前操作的人为负责人

        // 4. 记录初始归属
        createOwnerRecord(customer.getId(), null, userId, INITIAL_ASSIGN);

        // 5. 记录操作日志上下文
        LogRecordContext.putVariable("customer", customer);
        LogRecordContext.putVariable("duplicateCheckDecision", "");
        return customer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmCustomerImportRespVO importCustomerList(List<CrmCustomerImportExcelVO> importCustomers,
                                                      CrmCustomerImportReqVO importReqVO) {
        // 校验非空
        importCustomers = filterList(importCustomers, item -> Objects.nonNull(item.getName()));
        if (CollUtil.isEmpty(importCustomers)) {
            throw exception(CUSTOMER_IMPORT_LIST_IS_EMPTY);
        }

        // 逐条处理
        CrmCustomerImportRespVO respVO = CrmCustomerImportRespVO.builder().createCustomerNames(new ArrayList<>())
                .updateCustomerNames(new ArrayList<>()).failureCustomerNames(new LinkedHashMap<>()).build();
        importCustomers.forEach(importCustomer -> {
            // 校验，判断是否有不符合的原因
            try {
                validateCustomerForCreate(importCustomer);
            } catch (ServiceException ex) {
                respVO.getFailureCustomerNames().put(importCustomer.getName(), ex.getMessage());
                return;
            }
            // 情况一：判断如果不存在，在进行插入
            CrmCustomerDO existCustomer = customerMapper.selectByCustomerName(importCustomer.getName());
            if (existCustomer == null) {
                // 1.1 插入客户信息
                CrmCustomerDO customer = initCustomer(importCustomer, importReqVO.getOwnerUserId());
                customerMapper.insert(customer);
                respVO.getCreateCustomerNames().add(importCustomer.getName());
                // 1.2 创建数据权限
                if (importReqVO.getOwnerUserId() != null) {
                    permissionService.createPermission(new CrmPermissionCreateReqBO().setBizType(CrmBizTypeEnum.CRM_CUSTOMER.getType())
                            .setBizId(customer.getId()).setUserId(importReqVO.getOwnerUserId()).setLevel(CrmPermissionLevelEnum.OWNER.getLevel()));
                    createOwnerRecord(customer.getId(), null, importReqVO.getOwnerUserId(), INITIAL_ASSIGN);
                }
                // 1.3 记录操作日志
                getSelf().importCustomerLog(customer, false);
                return;
            }

            // 情况二：如果存在，判断是否允许更新
            if (!importReqVO.getUpdateSupport()) {
                respVO.getFailureCustomerNames().put(importCustomer.getName(),
                        StrUtil.format(CUSTOMER_NAME_EXISTS.getMsg(), importCustomer.getName()));
                return;
            }
            // 2.1 更新客户信息
            CrmCustomerDO updateCustomer = BeanUtils.toBean(importCustomer, CrmCustomerDO.class)
                    .setId(existCustomer.getId());
            customerMapper.updateById(updateCustomer);
            respVO.getUpdateCustomerNames().add(importCustomer.getName());
            // 2.2 记录操作日志
            getSelf().importCustomerLog(updateCustomer, true);
        });
        return respVO;
    }

    /**
     * 记录导入客户时的操作日志
     *
     * @param customer 客户信息
     * @param isUpdate 是否更新；true - 更新，false - 新增
     */
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_IMPORT_SUB_TYPE, bizNo = "{{#customer.id}}",
            success = CRM_CUSTOMER_IMPORT_SUCCESS)
    public void importCustomerLog(CrmCustomerDO customer, boolean isUpdate) {
        LogRecordContext.putVariable("customer", customer);
        LogRecordContext.putVariable("isUpdate", isUpdate);
    }

    // ==================== 公海相关操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_POOL_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_CUSTOMER_POOL_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#id", level = CrmPermissionLevelEnum.OWNER)
    public void putCustomerPool(Long id) {
        // 1. 校验存在
        CrmCustomerDO customer = customerMapper.selectById(id);
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        // 1.2. 校验是否为公海数据
        validateCustomerOwnerExists(customer, true);
        // 1.3. 校验客户是否锁定
        validateCustomerIsLocked(customer, true);

        // 2. 客户放入公海
        putCustomerPool(customer);

        // 记录操作日志上下文
        LogRecordContext.putVariable("customerName", customer.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveCustomer(List<Long> ids, Long ownerUserId, Boolean isReceive) {
        // 1.1 校验存在
        // 锁定客户并使并发领取/分配串行。后到的请求在获锁后会读到最新负责人并被拒绝
        List<CrmCustomerDO> customers = customerMapper.selectByIdsForUpdate(ids);
        if (customers.size() != ids.size()) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        // 1.2 校验负责人是否存在
        adminUserApi.validateUserList(singletonList(ownerUserId));
        // 1.3 校验状态
        customers.forEach(customer -> {
            // 校验是否已有负责人
            validateCustomerOwnerExists(customer, false);
            // 校验是否锁定
            validateCustomerIsLocked(customer, false);
            // 校验成交状态
            validateCustomerDeal(customer);
        });
        // 1.4  校验负责人是否到达上限
        validateCustomerExceedOwnerLimit(ownerUserId, customers.size());

        // 2. 领取公海数据
        List<CrmCustomerDO> updateCustomers = new ArrayList<>();
        List<CrmPermissionCreateReqBO> createPermissions = new ArrayList<>();
        customers.forEach(customer -> {
            // 2.1. 设置负责人
            updateCustomers.add(new CrmCustomerDO().setId(customer.getId())
                    .setOwnerUserId(ownerUserId).setOwnerTime(LocalDateTime.now()));
            // 2.2. 创建负责人数据权限
            createPermissions.add(new CrmPermissionCreateReqBO().setBizType(CrmBizTypeEnum.CRM_CUSTOMER.getType())
                    .setBizId(customer.getId()).setUserId(ownerUserId).setLevel(CrmPermissionLevelEnum.OWNER.getLevel()));
        });
        // 2.2 更新客户负责人
        customerMapper.updateBatch(updateCustomers);
        // 2.3 创建负责人数据权限
        permissionService.createPermissionBatch(createPermissions);
        // 2.4 记录领取/分配事件，供公海统计使用
        customerOwnerRecordMapper.insertBatch(CollectionUtils.convertList(customers, customer ->
                new CrmCustomerOwnerRecordDO().setCustomerId(customer.getId())
                        .setOwnerUserId(ownerUserId).setPreviousOwnerUserId(null).setNewOwnerUserId(ownerUserId)
                        .setType(TAKE_POOL.getType())));
        // TODO @芋艿：要不要处理关联的联系人？？？

        // 3. 记录操作日志
        AdminUserRespDTO user = null;
        if (!isReceive) {
            user = adminUserApi.getUser(ownerUserId);
        }
        for (CrmCustomerDO customer : customers) {
            getSelf().receiveCustomerLog(customer, user == null ? null : user.getNickname());
        }
    }

    @Override
    public int autoPutCustomerPool() {
        CrmCustomerPoolConfigDO poolConfig = customerPoolConfigService.getCustomerPoolConfig();
        if (poolConfig == null || !poolConfig.getEnabled()) {
            return 0;
        }
        // 1. 获得需要放到的客户列表
        List<CrmCustomerDO> customerList = customerMapper.selectListByAutoPool(poolConfig);
        // 2. 逐个放入公海
        int count = 0;
        for (CrmCustomerDO customer : customerList) {
            try {
                getSelf().putCustomerPool(customer);
                count++;
            } catch (Throwable e) {
                log.error("[autoPutCustomerPool][客户({}) 放入公海异常]", customer.getId(), e);
            }
        }
        return count;
    }

    @Transactional(rollbackFor = Exception.class) // 需要 protected 修饰，因为需要在事务中调用
    protected void putCustomerPool(CrmCustomerDO customer) {
        // 1. 记录进入公海事件；后续失败时随事务一起回滚
        customerOwnerRecordMapper.insert(new CrmCustomerOwnerRecordDO().setCustomerId(customer.getId())
                .setOwnerUserId(customer.getOwnerUserId()).setPreviousOwnerUserId(customer.getOwnerUserId())
                .setNewOwnerUserId(null).setType(PUT_POOL.getType()));

        // 2. 设置负责人为 NULL
        int updateOwnerUserIncr = customerMapper.updateOwnerUserIdById(customer.getId(), null);
        if (updateOwnerUserIncr == 0) {
            throw exception(CUSTOMER_UPDATE_OWNER_USER_FAIL);
        }

        // 3. 联系人的负责人，也要设置为 null。因为：因为领取后，负责人也要关联过来，这块和 receiveCustomer 是对应的
        contactService.updateOwnerUserIdByCustomerId(customer.getId(), null);

        // 4. 删除负责人数据权限
        // 注意：需要放在 contactService 后面，不然【客户】数据权限已经被删除，无法操作！
        permissionService.deletePermission(CrmBizTypeEnum.CRM_CUSTOMER.getType(), customer.getId(),
                CrmPermissionLevelEnum.OWNER.getLevel());
    }

    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_RECEIVE_SUB_TYPE, bizNo = "{{#customer.id}}",
            success = CRM_CUSTOMER_RECEIVE_SUCCESS)
    public void receiveCustomerLog(CrmCustomerDO customer, String ownerUserName) {
        // 记录操作日志上下文
        LogRecordContext.putVariable("customer", customer);
        LogRecordContext.putVariable("ownerUserName", ownerUserName);
    }

    //======================= 查询相关 =======================

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#customerId", level = CrmPermissionLevelEnum.READ)
    public List<CrmCustomerOwnerRecordDO> getCustomerOwnerRecordList(Long customerId) {
        validateCustomerExists(customerId);
        return customerOwnerRecordMapper.selectListByCustomerId(customerId);
    }

    /**
     * 创建客户归属变更记录。ownerUserId 保留为公海统计兼容字段。
     */
    private void createOwnerRecord(Long customerId, Long previousOwnerUserId, Long newOwnerUserId,
                                   CrmCustomerOwnerRecordTypeEnum type) {
        Long ownerUserId = type == PUT_POOL ? previousOwnerUserId : newOwnerUserId;
        customerOwnerRecordMapper.insert(new CrmCustomerOwnerRecordDO().setCustomerId(customerId)
                .setOwnerUserId(ownerUserId).setPreviousOwnerUserId(previousOwnerUserId)
                .setNewOwnerUserId(newOwnerUserId).setType(type.getType()));
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#id", level = CrmPermissionLevelEnum.READ)
    public CrmCustomerDO getCustomer(Long id) {
        return customerMapper.selectById(id);
    }

    @Override
    public List<CrmCustomerDO> getCustomerList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return customerMapper.selectByIds(ids);
    }

    @Override
    public List<CrmCustomerDO> getDuplicateCustomerList(CrmCustomerDuplicateCheckReqVO reqVO, Long userId) {
        String name = StrUtil.trim(reqVO.getName());
        String mobile = StrUtil.trim(reqVO.getMobile());
        if (StrUtil.isAllBlank(name, mobile)) {
            return Collections.emptyList();
        }
        return customerMapper.selectDuplicateList(name, mobile, reqVO.getExcludeId(), userId);
    }

    @Override
    public PageResult<CrmCustomerDO> getCustomerPage(CrmCustomerPageReqVO pageReqVO, Long userId) {
        return customerMapper.selectPage(pageReqVO, userId);
    }

    @Override
    public PageResult<CrmCustomerDO> getPutPoolRemindCustomerPage(CrmCustomerPageReqVO pageVO, Long userId) {
        CrmCustomerPoolConfigDO poolConfig = customerPoolConfigService.getCustomerPoolConfig();
        if (ObjUtil.isNull(poolConfig)
                || Boolean.FALSE.equals(poolConfig.getEnabled())
                || Boolean.FALSE.equals(poolConfig.getNotifyEnabled())) {
            return PageResult.empty();
        }
        return customerMapper.selectPutPoolRemindCustomerPage(pageVO, poolConfig, userId);
    }

    @Override
    public Long getPutPoolRemindCustomerCount(Long userId) {
        CrmCustomerPoolConfigDO poolConfig = customerPoolConfigService.getCustomerPoolConfig();
        if (ObjUtil.isNull(poolConfig)
                || Boolean.FALSE.equals(poolConfig.getEnabled())
                || Boolean.FALSE.equals(poolConfig.getNotifyEnabled())) {
            return 0L;
        }
        CrmCustomerPageReqVO pageVO = new CrmCustomerPageReqVO()
                .setPool(null)
                .setContactStatus(CrmCustomerPageReqVO.CONTACT_TODAY)
                .setSceneType(CrmSceneTypeEnum.OWNER.getType());
        return customerMapper.selectPutPoolRemindCustomerCount(pageVO, poolConfig, userId);
    }

    @Override
    public Long getTodayContactCustomerCount(Long userId) {
        return customerMapper.selectCountByTodayContact(userId);
    }

    @Override
    public Long getFollowCustomerCount(Long userId) {
        return customerMapper.selectCountByFollow(userId);
    }

    // ======================= 校验相关 =======================

    private void validateCustomerForCreate(CrmCustomerImportExcelVO importCustomer) {
        // 校验客户名称不能为空
        if (StrUtil.isEmptyIfStr(importCustomer.getName())) {
            throw exception(CUSTOMER_CREATE_NAME_NOT_NULL);
        }
    }

    /**
     * 校验客户名称唯一。客户导入支持显式覆盖，继续使用其独立分支处理。
     *
     * @param id   更新时的客户编号；创建时为 {@code null}
     * @param name 客户名称
     */
    private void validateCustomerNameUnique(Long id, String name) {
        CrmCustomerDO customer = customerMapper.selectByCustomerName(name);
        if (customer != null && !ObjUtil.equal(customer.getId(), id)) {
            throw exception(CUSTOMER_NAME_EXISTS, name);
        }
    }

    /**
     * 校验客户是否被引用
     *
     * @param id 客户编号
     */
    private void validateCustomerReference(Long id, List<CrmCustomerDO> hierarchy) {
        if (hierarchy.stream().anyMatch(customer -> ObjUtil.equal(customer.getParentCustomerId(), id))) {
            throw exception(CUSTOMER_DELETE_FAIL_HAVE_REFERENCE, "下级客户");
        }
        if (contactService.getContactCountByCustomerId(id) > 0) {
            throw exception(CUSTOMER_DELETE_FAIL_HAVE_REFERENCE, CrmBizTypeEnum.CRM_CONTACT.getName());
        }
        if (businessService.getBusinessCountByCustomerId(id) > 0) {
            throw exception(CUSTOMER_DELETE_FAIL_HAVE_REFERENCE, CrmBizTypeEnum.CRM_BUSINESS.getName());
        }
        if (contractService.getContractCountByCustomerId(id) > 0) {
            throw exception(CUSTOMER_DELETE_FAIL_HAVE_REFERENCE, CrmBizTypeEnum.CRM_CONTRACT.getName());
        }
    }

    /**
     * 校验上级客户关系。传入列表必须来自 {@link CrmCustomerMapper#selectHierarchyListForUpdate()}，
     * 从而使校验和后续写入共享同一个受锁保护的层级快照。
     */
    private void validateCustomerHierarchy(Long customerId, Long parentCustomerId, List<CrmCustomerDO> hierarchy) {
        Map<Long, Long> parentMap = new HashMap<>(hierarchy.size());
        hierarchy.forEach(customer -> parentMap.put(customer.getId(), customer.getParentCustomerId()));
        if (customerId != null && !parentMap.containsKey(customerId)) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        if (parentCustomerId == null) {
            return;
        }
        if (ObjUtil.equal(customerId, parentCustomerId)) {
            throw exception(CUSTOMER_PARENT_SELF);
        }
        if (!parentMap.containsKey(parentCustomerId)) {
            throw exception(CUSTOMER_PARENT_NOT_EXISTS);
        }

        Set<Long> visited = new HashSet<>();
        Long ancestorId = parentCustomerId;
        while (ancestorId != null) {
            if (ObjUtil.equal(ancestorId, customerId) || !visited.add(ancestorId)) {
                throw exception(CUSTOMER_HIERARCHY_CYCLE);
            }
            ancestorId = parentMap.get(ancestorId);
        }
    }

    /**
     * 校验客户是否存在
     *
     * @param id 客户 id
     */
    @Override
    public void validateCustomer(Long id) {
        validateCustomerExists(id);
    }

    private void validateCustomerOwnerExists(CrmCustomerDO customer, Boolean pool) {
        if (customer == null) { // 防御一下
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        // 校验是否为公海数据
        if (pool && customer.getOwnerUserId() == null) {
            throw exception(CUSTOMER_IN_POOL, customer.getName());
        }
        // 负责人已存在
        if (!pool && customer.getOwnerUserId() != null) {
            throw exception(CUSTOMER_OWNER_EXISTS, customer.getName());
        }
    }

    private CrmCustomerDO validateCustomerExists(Long id) {
        CrmCustomerDO customerDO = customerMapper.selectById(id);
        if (customerDO == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        return customerDO;
    }

    private void validateCustomerIsLocked(CrmCustomerDO customer, Boolean pool) {
        if (customer.getLockStatus()) {
            throw exception(pool ? CUSTOMER_LOCKED_PUT_POOL_FAIL : CUSTOMER_LOCKED, customer.getName());
        }
    }

    private void validateCustomerDeal(CrmCustomerDO customer) {
        if (customer.getDealStatus()) {
            throw exception(CUSTOMER_ALREADY_DEAL);
        }
    }

    /**
     * 校验用户拥有的客户数量，是否到达上限
     *
     * @param userId   用户编号
     * @param newCount 附加数量
     */
    private void validateCustomerExceedOwnerLimit(Long userId, int newCount) {
        List<CrmCustomerLimitConfigDO> limitConfigs = customerLimitConfigService.getCustomerLimitConfigListByUserId(
                CUSTOMER_OWNER_LIMIT.getType(), userId);
        if (CollUtil.isEmpty(limitConfigs)) {
            return;
        }
        Long ownerCount = customerMapper.selectCountByDealStatusAndOwnerUserId(null, userId);
        Long dealOwnerCount = customerMapper.selectCountByDealStatusAndOwnerUserId(true, userId);
        limitConfigs.forEach(limitConfig -> {
            long nowCount = limitConfig.getDealCountEnabled() ? ownerCount : ownerCount - dealOwnerCount;
            if (nowCount + newCount > limitConfig.getMaxCount()) {
                throw exception(CUSTOMER_OWNER_EXCEED_LIMIT);
            }
        });
    }

    /**
     * 校验用户锁定的客户数量，是否到达上限
     *
     * @param userId 用户编号
     */
    private void validateCustomerExceedLockLimit(Long userId) {
        List<CrmCustomerLimitConfigDO> limitConfigs = customerLimitConfigService.getCustomerLimitConfigListByUserId(
                CUSTOMER_LOCK_LIMIT.getType(), userId);
        if (CollUtil.isEmpty(limitConfigs)) {
            return;
        }
        Long lockCount = customerMapper.selectCountByLockStatusAndOwnerUserId(true, userId);
        Integer maxCount = CollectionUtils.getMaxValue(limitConfigs, CrmCustomerLimitConfigDO::getMaxCount);
        assert maxCount != null;
        if (lockCount >= maxCount) {
            throw exception(CUSTOMER_LOCK_EXCEED_LIMIT);
        }
    }

    /**
     * 获得自身的代理对象，解决 AOP 生效问题
     *
     * @return 自己
     */
    private CrmCustomerServiceImpl getSelf() {
        return SpringUtil.getBean(getClass());
    }

}

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
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolConfigDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolReceiveDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolRuleDO;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerPoolReceiveMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerPoolRuleMapper;
import com.meession.etm.module.crm.service.customer.bo.CrmCustomerPoolReceiveRuleConfig;
import com.meession.etm.module.crm.service.customer.rule.PoolReceiveRule;
import com.meession.etm.module.crm.enums.customer.CrmCustomerPoolReceiveSourceTypeEnum;
import com.meession.etm.module.crm.enums.customer.CrmCustomerPoolRuleTypeEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.common.CrmSceneTypeEnum;
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

    @Resource
    private CrmCustomerPoolReceiveMapper poolReceiveMapper;

    @Resource
    private CrmCustomerPoolRuleMapper poolRuleMapper;

    @Resource
    @Lazy
    private PoolReceiveRule poolReceiveRule;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_CREATE_SUB_TYPE, bizNo = "{{#customer.id}}",
            success = CRM_CUSTOMER_CREATE_SUCCESS)
    public Long createCustomer(CrmCustomerSaveReqVO createReqVO, Long userId) {
        createReqVO.setId(null);
        // 1. 校验拥有客户是否到达上限
        validateCustomerExceedOwnerLimit(createReqVO.getOwnerUserId(), 1);

        // 2. 插入客户
        CrmCustomerDO customer = initCustomer(createReqVO, userId);
        customerMapper.insert(customer);

        // 3. 创建数据权限
        permissionService.createPermission(new CrmPermissionCreateReqBO().setBizType(CrmBizTypeEnum.CRM_CUSTOMER.getType())
                .setBizId(customer.getId()).setUserId(userId).setLevel(CrmPermissionLevelEnum.OWNER.getLevel())); // 设置当前操作的人为负责人

        // 4. 记录操作日志上下文
        LogRecordContext.putVariable("customer", customer);
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
                .setOwnerTime(LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_UPDATE_SUB_TYPE, bizNo = "{{#updateReqVO.id}}",
            success = CRM_CUSTOMER_UPDATE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#updateReqVO.id", level = CrmPermissionLevelEnum.WRITE)
    public void updateCustomer(CrmCustomerSaveReqVO updateReqVO) {
        Assert.notNull(updateReqVO.getId(), "客户编号不能为空");
        updateReqVO.setOwnerUserId(null);  // 更新的时候，要把 updateReqVO 负责人设置为空，避免修改
        // 1. 校验存在
        CrmCustomerDO oldCustomer = validateCustomerExists(updateReqVO.getId());

        // 2. 更新客户
        CrmCustomerDO updateObj = BeanUtils.toBean(updateReqVO, CrmCustomerDO.class);
        customerMapper.updateById(updateObj);

        // 3. 记录操作日志上下文
        updateReqVO.setOwnerUserId(oldCustomer.getOwnerUserId()); // 避免操作日志出现“删除负责人”的情况
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtils.toBean(oldCustomer, CrmCustomerSaveReqVO.class));
        LogRecordContext.putVariable("customerName", oldCustomer.getName());
    }

    @Override
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_UPDATE_DEAL_STATUS_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_CUSTOMER_UPDATE_DEAL_STATUS_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#id", level = CrmPermissionLevelEnum.WRITE)
    public void updateCustomerDealStatus(Long id, Boolean dealStatus) {
        // 1.1 校验存在
        CrmCustomerDO customer = validateCustomerExists(id);
        // 1.2 校验是否重复操作
        if (Objects.equals(customer.getDealStatus(), dealStatus)) {
            throw exception(CUSTOMER_UPDATE_DEAL_STATUS_FAIL);
        }

        // 2. 更新客户的成交状态
        customerMapper.updateById(new CrmCustomerDO().setId(id).setDealStatus(dealStatus));

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("customerName", customer.getName());
        LogRecordContext.putVariable("dealStatus", dealStatus);
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
        // 1.1 校验存在
        CrmCustomerDO customer = validateCustomerExists(id);
        // 1.2 检查引用
        validateCustomerReference(id);

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

        // 3. 记录转移日志
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
        // 1. 插入客户
        CrmCustomerDO customer = initCustomer(createReqBO, userId);
        customerMapper.insert(customer);

        // 2. 创建数据权限
        permissionService.createPermission(new CrmPermissionCreateReqBO().setBizType(CrmBizTypeEnum.CRM_CUSTOMER.getType())
                .setBizId(customer.getId()).setUserId(userId).setLevel(CrmPermissionLevelEnum.OWNER.getLevel())); // 设置当前操作的人为负责人

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("customer", customer);
        return customer.getId();
    }

    @Override
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
        putCustomerPool(customer, "手动放入公海", null);

        // 记录操作日志上下文
        LogRecordContext.putVariable("customerName", customer.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveCustomer(List<Long> ids, Long ownerUserId, Boolean isReceive) {
        // 1.1 校验存在
        List<CrmCustomerDO> customers = customerMapper.selectByIds(ids);
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
        // 1.4 校验负责人是否到达上限
        validateCustomerExceedOwnerLimit(ownerUserId, customers.size());

        // 1.5 校验领取数量限制（仅手动领取时校验）
        if (Boolean.TRUE.equals(isReceive)) {
            validateReceiveLimit(ownerUserId, customers.size());
        }

        // 2. 领取公海数据
        List<CrmPermissionCreateReqBO> createPermissions = new ArrayList<>();
        List<CrmCustomerPoolReceiveDO> receiveRecords = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Integer sourceType = Boolean.TRUE.equals(isReceive)
                ? CrmCustomerPoolReceiveSourceTypeEnum.MANUAL.getType()
                : CrmCustomerPoolReceiveSourceTypeEnum.ADMIN.getType();
        
        // 获取领取冻结期配置
        Integer freezeDays = getReceiveFreezeDays();
        LocalDateTime receiveFreezeEndTime = freezeDays != null && freezeDays > 0 
                ? now.plusDays(freezeDays) : null;

        customers.forEach(customer -> {
            // 2.1. 更新客户负责人和公海状态
            customerMapper.updateReceiveInfo(customer.getId(), ownerUserId, now, now, receiveFreezeEndTime);
            // 2.2. 创建负责人数据权限
            createPermissions.add(new CrmPermissionCreateReqBO().setBizType(CrmBizTypeEnum.CRM_CUSTOMER.getType())
                    .setBizId(customer.getId()).setUserId(ownerUserId).setLevel(CrmPermissionLevelEnum.OWNER.getLevel()));
            // 2.3. 创建领取记录
            receiveRecords.add(CrmCustomerPoolReceiveDO.builder()
                    .customerId(customer.getId())
                    .receiveUserId(ownerUserId)
                    .receiveTime(now)
                    .sourceType(sourceType)
                    .build());
            // 2.4. 更新关联联系人负责人
            contactService.updateOwnerUserIdByCustomerId(customer.getId(), ownerUserId);
        });
        // 2.5 创建负责人数据权限
        permissionService.createPermissionBatch(createPermissions);
        // 2.6 保存领取记录
        poolReceiveMapper.insertBatch(receiveRecords);

        // 3. 记录操作日志
        AdminUserRespDTO user = null;
        if (!isReceive) {
            user = adminUserApi.getUser(ownerUserId);
        }
        for (CrmCustomerDO customer : customers) {
            getSelf().receiveCustomerLog(customer, user == null ? null : user.getNickname());
        }
    }

    private void validateReceiveLimit(Long userId, int count) {
        List<CrmCustomerPoolRuleDO> receiveRules = poolRuleMapper.selectReceiveRules();
        for (CrmCustomerPoolRuleDO rule : receiveRules) {
            CrmCustomerPoolReceiveRuleConfig config = poolReceiveRule.parseConfig(rule, CrmCustomerPoolReceiveRuleConfig.class);
            if (!poolReceiveRule.checkLimit(userId, count, config)) {
                throw exception(CUSTOMER_RECEIVE_LIMIT_EXCEED);
            }
        }
    }

    private Integer getReceiveFreezeDays() {
        List<CrmCustomerPoolRuleDO> receiveRules = poolRuleMapper.selectReceiveRules();
        for (CrmCustomerPoolRuleDO rule : receiveRules) {
            CrmCustomerPoolReceiveRuleConfig config = poolReceiveRule.parseConfig(rule, CrmCustomerPoolReceiveRuleConfig.class);
            if (config != null && config.getFreezeDays() != null && config.getFreezeDays() > 0) {
                return config.getFreezeDays();
            }
        }
        return null;
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
                getSelf().putCustomerPool(customer, "自动回收-超时", null);
                getSelf().autoPutCustomerPoolLog(customer, "自动回收-超时");
                count++;
            } catch (Throwable e) {
                log.error("[autoPutCustomerPool][客户({}) 放入公海异常]", customer.getId(), e);
            }
        }
        return count;
    }

    @Transactional(rollbackFor = Exception.class)
    protected void putCustomerPool(CrmCustomerDO customer, String poolReason, Long poolRuleId) {
        LocalDateTime now = LocalDateTime.now();
        
        // 1. 设置负责人为 NULL
        int updateOwnerUserIncr = customerMapper.updateOwnerUserIdById(customer.getId(), null);
        if (updateOwnerUserIncr == 0) {
            throw exception(CUSTOMER_UPDATE_OWNER_USER_FAIL);
        }

        // 2. 更新公海状态字段
        int updatePoolStatusIncr = customerMapper.updatePoolStatus(customer.getId(), 1, now, poolReason, poolRuleId);
        if (updatePoolStatusIncr == 0) {
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

    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_AUTO_POOL_SUB_TYPE, bizNo = "{{#customer.id}}",
            success = CRM_CUSTOMER_AUTO_POOL_SUCCESS)
    public void autoPutCustomerPoolLog(CrmCustomerDO customer, String poolReason) {
        LogRecordContext.putVariable("customer", customer);
        LogRecordContext.putVariable("customerName", customer.getName());
        LogRecordContext.putVariable("poolReason", poolReason);
    }

    //======================= 查询相关 =======================

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
     * 校验客户是否被引用
     *
     * @param id 客户编号
     */
    private void validateCustomerReference(Long id) {
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

    @Override
    public CrmCustomerDuplicateCheckRespVO checkDuplicate(CrmCustomerDuplicateCheckReqVO reqVO) {
        List<CrmCustomerDO> customers = customerMapper.selectDuplicateCustomers(
                reqVO.getName(), reqVO.getMobile(), reqVO.getTelephone(),
                reqVO.getEmail(), reqVO.getQq(), reqVO.getWechat(), reqVO.getStrictMatch());

        if (CollUtil.isEmpty(customers)) {
            return CrmCustomerDuplicateCheckRespVO.builder()
                    .hasDuplicate(false)
                    .duplicates(Collections.emptyList())
                    .build();
        }

        List<CrmCustomerDuplicateCheckRespVO.DuplicateCustomer> duplicates = new ArrayList<>();
        for (CrmCustomerDO customer : customers) {
            DuplicateCustomerMatchResult matchResult = calculateMatchScore(customer, reqVO);
            if (matchResult.getMatchScore() > 0) {
                duplicates.add(CrmCustomerDuplicateCheckRespVO.DuplicateCustomer.builder()
                        .id(customer.getId())
                        .name(customer.getName())
                        .mobile(customer.getMobile())
                        .telephone(customer.getTelephone())
                        .email(customer.getEmail())
                        .qq(customer.getQq())
                        .wechat(customer.getWechat())
                        .matchScore(matchResult.getMatchScore())
                        .matchedFields(matchResult.getMatchedFields())
                        .build());
            }
        }

        duplicates.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));

        return CrmCustomerDuplicateCheckRespVO.builder()
                .hasDuplicate(!duplicates.isEmpty())
                .duplicates(duplicates)
                .build();
    }

    private DuplicateCustomerMatchResult calculateMatchScore(CrmCustomerDO customer, CrmCustomerDuplicateCheckReqVO reqVO) {
        int score = 0;
        List<String> matchedFields = new ArrayList<>();

        if (StrUtil.isNotEmpty(reqVO.getName()) && StrUtil.isNotEmpty(customer.getName())) {
            if (customer.getName().equalsIgnoreCase(reqVO.getName())) {
                score += 40;
                matchedFields.add("name");
            } else if (customer.getName().toLowerCase().contains(reqVO.getName().toLowerCase())) {
                score += 20;
                matchedFields.add("name");
            }
        }

        if (StrUtil.isNotEmpty(reqVO.getMobile()) && StrUtil.isNotEmpty(customer.getMobile())) {
            if (customer.getMobile().equals(reqVO.getMobile())) {
                score += 30;
                matchedFields.add("mobile");
            }
        }

        if (StrUtil.isNotEmpty(reqVO.getTelephone()) && StrUtil.isNotEmpty(customer.getTelephone())) {
            if (customer.getTelephone().equals(reqVO.getTelephone())) {
                score += 30;
                matchedFields.add("telephone");
            }
        }

        if (StrUtil.isNotEmpty(reqVO.getEmail()) && StrUtil.isNotEmpty(customer.getEmail())) {
            if (customer.getEmail().equalsIgnoreCase(reqVO.getEmail())) {
                score += 25;
                matchedFields.add("email");
            }
        }

        if (StrUtil.isNotEmpty(reqVO.getQq()) && StrUtil.isNotEmpty(customer.getQq())) {
            if (customer.getQq().equals(reqVO.getQq())) {
                score += 20;
                matchedFields.add("qq");
            }
        }

        if (StrUtil.isNotEmpty(reqVO.getWechat()) && StrUtil.isNotEmpty(customer.getWechat())) {
            if (customer.getWechat().equalsIgnoreCase(reqVO.getWechat())) {
                score += 20;
                matchedFields.add("wechat");
            }
        }

        return new DuplicateCustomerMatchResult(score, matchedFields);
    }

    private static class DuplicateCustomerMatchResult {
        private final int matchScore;
        private final List<String> matchedFields;

        public DuplicateCustomerMatchResult(int matchScore, List<String> matchedFields) {
            this.matchScore = matchScore;
            this.matchedFields = matchedFields;
        }

        public int getMatchScore() {
            return matchScore;
        }

        public List<String> getMatchedFields() {
            return matchedFields;
        }
    }

    // ==================== 星级评估 ====================

    @Override
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_STAR_ASSESSMENT_SUB_TYPE, bizNo = "{{#reqVO.id}}",
            success = CRM_CUSTOMER_STAR_ASSESSMENT_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#reqVO.id", level = CrmPermissionLevelEnum.WRITE)
    public CrmCustomerStarAssessmentRespVO assessCustomerStar(CrmCustomerStarAssessmentReqVO reqVO, Long userId) {
        CrmCustomerDO customer = validateCustomerExists(reqVO.getId());

        customerMapper.updateById(new CrmCustomerDO().setId(reqVO.getId()).setLevel(reqVO.getStar()));

        AdminUserRespDTO assessor = adminUserApi.getUser(userId);

        CrmCustomerStarAssessmentRespVO respVO = buildStarAssessmentRespVO(customer, reqVO.getStar(), reqVO.getRemark(), assessor);

        LogRecordContext.putVariable("customerName", customer.getName());
        LogRecordContext.putVariable("star", reqVO.getStar());

        return respVO;
    }

    @Override
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_STAR_ASSESSMENT_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_CUSTOMER_STAR_ASSESSMENT_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#id", level = CrmPermissionLevelEnum.WRITE)
    public CrmCustomerStarAssessmentRespVO autoAssessCustomerStar(Long id, Long userId) {
        CrmCustomerDO customer = validateCustomerExists(id);

        AssessmentScore score = calculateCustomerScore(customer);

        int star = calculateStar(score.getTotalScore());

        customerMapper.updateById(new CrmCustomerDO().setId(id).setLevel(star));

        AdminUserRespDTO assessor = adminUserApi.getUser(userId);

        CrmCustomerStarAssessmentRespVO.AssessmentDimension dimension = new CrmCustomerStarAssessmentRespVO.AssessmentDimension();
        dimension.setDealAmountScore(score.getDealAmountScore());
        dimension.setDealCountScore(score.getDealCountScore());
        dimension.setFollowScore(score.getFollowScore());
        dimension.setLevelScore(score.getLevelScore());
        dimension.setSourceScore(score.getSourceScore());
        dimension.setStatusScore(score.getStatusScore());

        CrmCustomerStarAssessmentRespVO respVO = buildStarAssessmentRespVO(customer, star, "系统自动评估", assessor);
        respVO.setScore(score.getTotalScore());
        respVO.setDimension(dimension);

        LogRecordContext.putVariable("customerName", customer.getName());
        LogRecordContext.putVariable("star", star);

        return respVO;
    }

    private AssessmentScore calculateCustomerScore(CrmCustomerDO customer) {
        AssessmentScore score = new AssessmentScore();

        Long totalDealAmount = businessService.getTotalDealAmountByCustomerId(customer.getId());
        if (totalDealAmount != null && totalDealAmount > 0) {
            if (totalDealAmount >= 1000000) {
                score.dealAmountScore = 25;
            } else if (totalDealAmount >= 500000) {
                score.dealAmountScore = 20;
            } else if (totalDealAmount >= 100000) {
                score.dealAmountScore = 15;
            } else if (totalDealAmount >= 10000) {
                score.dealAmountScore = 10;
            } else {
                score.dealAmountScore = 5;
            }
        }

        int dealCount = businessService.getBusinessCountByCustomerId(customer.getId());
        if (dealCount >= 5) {
            score.dealCountScore = 20;
        } else if (dealCount >= 3) {
            score.dealCountScore = 15;
        } else if (dealCount >= 1) {
            score.dealCountScore = 10;
        }

        if (customer.getContactLastTime() != null) {
            LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            if (customer.getContactLastTime().isAfter(oneMonthAgo)) {
                score.followScore = 20;
            } else if (customer.getContactLastTime().isAfter(threeMonthsAgo)) {
                score.followScore = 15;
            } else {
                score.followScore = 5;
            }
        }

        if (customer.getLevel() != null) {
            if (customer.getLevel() == 1) {
                score.levelScore = 15;
            } else if (customer.getLevel() == 2) {
                score.levelScore = 10;
            } else {
                score.levelScore = 5;
            }
        }

        if (customer.getSource() != null) {
            score.sourceScore = 10;
        }

        if (customer.getStatus() != null) {
            if (customer.getStatus() == 1) {
                score.statusScore = 10;
            } else if (customer.getStatus() == 2) {
                score.statusScore = 8;
            } else {
                score.statusScore = 5;
            }
        }

        return score;
    }

    private int calculateStar(int totalScore) {
        if (totalScore >= 90) {
            return 5;
        } else if (totalScore >= 75) {
            return 4;
        } else if (totalScore >= 60) {
            return 3;
        } else if (totalScore >= 40) {
            return 2;
        } else {
            return 1;
        }
    }

    private CrmCustomerStarAssessmentRespVO buildStarAssessmentRespVO(CrmCustomerDO customer, Integer star, String remark, AdminUserRespDTO assessor) {
        CrmCustomerStarAssessmentRespVO respVO = new CrmCustomerStarAssessmentRespVO();
        respVO.setId(customer.getId());
        respVO.setName(customer.getName());
        respVO.setStar(star);
        respVO.setStarName(getStarName(star));
        respVO.setRemark(remark);
        respVO.setAssessmentTime(LocalDateTime.now());
        if (assessor != null) {
            respVO.setAssessorName(assessor.getNickname());
        }
        return respVO;
    }

    private String getStarName(Integer star) {
        return switch (star) {
            case 1 -> "一星客户";
            case 2 -> "二星客户";
            case 3 -> "三星客户";
            case 4 -> "四星客户";
            case 5 -> "五星客户";
            default -> "未评级";
        };
    }

    private static class AssessmentScore {
        private int dealAmountScore = 0;
        private int dealCountScore = 0;
        private int followScore = 0;
        private int levelScore = 0;
        private int sourceScore = 0;
        private int statusScore = 0;

        public int getTotalScore() {
            return dealAmountScore + dealCountScore + followScore + levelScore + sourceScore + statusScore;
        }

        public int getDealAmountScore() {
            return dealAmountScore;
        }

        public int getDealCountScore() {
            return dealCountScore;
        }

        public int getFollowScore() {
            return followScore;
        }

        public int getLevelScore() {
            return levelScore;
        }

        public int getSourceScore() {
            return sourceScore;
        }

        public int getStatusScore() {
            return statusScore;
        }
    }

}

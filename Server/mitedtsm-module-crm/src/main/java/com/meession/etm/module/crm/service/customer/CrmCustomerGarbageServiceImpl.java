package com.meession.etm.module.crm.service.customer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.collection.CollectionUtils;
import com.meession.etm.framework.tenant.core.context.TenantContextHolder;
import com.meession.etm.module.crm.controller.admin.customer.vo.garbage.CrmCustomerGarbagePageReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.garbage.CrmCustomerGarbagePutReqVO;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerOwnerRecordDO;
import com.meession.etm.module.crm.dal.mysql.business.CrmBusinessMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerOwnerRecordMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerReferenceMapper;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.customer.CrmCustomerOwnerRecordSourceEnum;
import com.meession.etm.module.crm.enums.customer.CrmCustomerOwnerRecordTypeEnum;
import com.meession.etm.module.crm.enums.customer.CrmCustomerPoolStatusEnum;
import com.meession.etm.module.crm.framework.permission.CrmAuthorizationService;
import com.meession.etm.module.crm.framework.pool.CrmPoolPolicyProperties;
import com.meession.etm.module.crm.framework.pool.CrmPoolTimeProvider;
import com.meession.etm.module.crm.service.contact.CrmContactService;
import com.meession.etm.module.crm.service.followup.CrmFollowUpRecordService;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static com.meession.etm.module.crm.enums.LogRecordConstants.*;

@Service
@Validated
@Slf4j
public class CrmCustomerGarbageServiceImpl implements CrmCustomerGarbageService {

    @Resource
    private CrmCustomerMapper customerMapper;
    @Resource
    private CrmCustomerOwnerRecordMapper customerOwnerRecordMapper;
    @Resource
    private CrmCustomerReferenceMapper customerReferenceMapper;
    @Resource
    private CrmBusinessMapper businessMapper;
    @Resource
    private CrmContractMapper contractMapper;
    @Resource
    private CrmContactService contactService;
    @Resource
    private CrmPermissionService permissionService;
    @Resource
    private CrmFollowUpRecordService followUpRecordService;
    @Resource
    private CrmAuthorizationService authorizationService;
    @Resource
    private CrmPoolPolicyProperties poolPolicyProperties;
    @Resource
    private CrmPoolTimeProvider poolTimeProvider;

    @Override
    public PageResult<CrmCustomerDO> getGarbagePage(CrmCustomerGarbagePageReqVO pageReqVO, Long userId) {
        validateGarbageAdmin(userId);
        return customerMapper.selectGarbagePage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_GARBAGE_SUB_TYPE,
            bizNo = "{{#reqVO.customerId}}", success = CRM_CUSTOMER_GARBAGE_SUCCESS)
    public void putCustomerGarbage(CrmCustomerGarbagePutReqVO reqVO, Long userId) {
        validateGarbageAdmin(userId);
        CrmCustomerDO customer = customerMapper.selectByIdForUpdate(reqVO.getCustomerId());
        validatePublicCustomer(customer);
        LocalDateTime now = poolTimeProvider.now();
        validateGarbageProtection(customer, now);
        putCustomerGarbage(customer, reqVO.getReason().trim(),
                CrmCustomerOwnerRecordSourceEnum.MANUAL_GARBAGE.getSource(), now);
        LogRecordContext.putVariable("customerName", customer.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_GARBAGE_RESTORE_SUB_TYPE,
            bizNo = "{{#customerId}}", success = CRM_CUSTOMER_GARBAGE_RESTORE_SUCCESS)
    public void restoreCustomerToPublicPool(Long customerId, Long userId) {
        validateGarbageAdmin(userId);
        CrmCustomerDO customer = customerMapper.selectByIdForUpdate(customerId);
        validateGarbageCustomer(customer);
        LocalDateTime now = poolTimeProvider.now();
        if (customerMapper.updateGarbageToPublic(customerId, now,
                CrmCustomerOwnerRecordSourceEnum.RESTORE_PUBLIC.getSource()) == 0) {
            throw exception(CUSTOMER_GARBAGE_STATE_INVALID);
        }
        customerOwnerRecordMapper.insert(new CrmCustomerOwnerRecordDO().setCustomerId(customerId)
                .setPreviousOwnerUserId(null).setNewOwnerUserId(null)
                .setType(CrmCustomerOwnerRecordTypeEnum.RESTORE_PUBLIC.getType())
                .setSource(CrmCustomerOwnerRecordSourceEnum.RESTORE_PUBLIC.getSource())
                .setReason("从垃圾池恢复到公海"));
        LogRecordContext.putVariable("customerName", customer.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CUSTOMER_TYPE, subType = CRM_CUSTOMER_GARBAGE_DELETE_SUB_TYPE,
            bizNo = "{{#customerId}}", success = CRM_CUSTOMER_GARBAGE_DELETE_SUCCESS)
    public void permanentlyDeleteGarbageCustomer(Long customerId, Long userId) {
        validateGarbageAdmin(userId);
        CrmCustomerDO customer = customerMapper.selectByIdForUpdate(customerId);
        validateGarbageCustomer(customer);
        String reference = customerReferenceMapper.selectFirstReference(
                TenantContextHolder.getRequiredTenantId(), customerId);
        if (reference != null) {
            throw exception(CUSTOMER_GARBAGE_DELETE_REFERENCED, reference);
        }
        permissionService.deletePermissionIfPresent(CrmBizTypeEnum.CRM_CUSTOMER.getType(), customerId);
        followUpRecordService.deleteFollowUpRecordByBiz(CrmBizTypeEnum.CRM_CUSTOMER.getType(), customerId);
        customerOwnerRecordMapper.insert(new CrmCustomerOwnerRecordDO().setCustomerId(customerId)
                .setPreviousOwnerUserId(null).setNewOwnerUserId(null)
                .setType(CrmCustomerOwnerRecordTypeEnum.DELETE_GARBAGE.getType())
                .setSource(CrmCustomerOwnerRecordSourceEnum.MANUAL_GARBAGE.getSource())
                .setReason("垃圾池永久删除"));
        if (customerMapper.deleteGarbagePermanently(TenantContextHolder.getRequiredTenantId(), customerId) == 0) {
            throw exception(CUSTOMER_GARBAGE_STATE_INVALID);
        }
        LogRecordContext.putVariable("customerName", customer.getName());
    }

    @Override
    public int autoPutCustomerGarbage() {
        CrmPoolPolicyProperties.Garbage policy = poolPolicyProperties.getGarbage();
        if (!policy.isAutoEnabled()) {
            return 0;
        }
        LocalDateTime now = poolTimeProvider.now();
        LocalDateTime expireBefore = now.minusDays(policy.getExpireDays());
        int scanSize = Math.min(policy.getBatchSize(), policy.getMaxBatchSize());
        long afterId = 0L;
        int count = 0;
        for (int batch = 0; batch < policy.getMaxBatches(); batch++) {
            List<CrmCustomerDO> candidates = customerMapper.selectListByAutoGarbage(afterId, expireBefore,
                    policy.getMinimumPoolCycles(), scanSize, policy.getMaxBatchSize());
            if (CollUtil.isEmpty(candidates)) {
                break;
            }
            afterId = candidates.get(candidates.size() - 1).getId();
            Set<Long> ids = CollectionUtils.convertSet(candidates, CrmCustomerDO::getId);
            Set<Long> activeBusinessIds = businessMapper.selectActiveCustomerIds(ids);
            Set<Long> protectedContractIds = contractMapper.selectProtectedCustomerIds(ids,
                    poolPolicyProperties.getCustomer().getProtectedContractAuditStatuses(), now);
            for (CrmCustomerDO candidate : candidates) {
                if (activeBusinessIds.contains(candidate.getId()) || protectedContractIds.contains(candidate.getId())) {
                    continue;
                }
                try {
                    if (getSelf().autoPutSingleCustomerGarbage(candidate.getId(), expireBefore,
                            policy.getMinimumPoolCycles(), now)) {
                        count++;
                    }
                } catch (RuntimeException ex) {
                    log.error("[autoPutCustomerGarbage][customerId({}) failed]", candidate.getId(), ex);
                }
            }
            if (candidates.size() < scanSize) {
                break;
            }
        }
        return count;
    }

    @Transactional(rollbackFor = Exception.class)
    protected boolean autoPutSingleCustomerGarbage(Long customerId, LocalDateTime expireBefore,
                                                    int minimumPoolCycles, LocalDateTime now) {
        CrmCustomerDO customer = customerMapper.selectByIdForUpdate(customerId);
        if (customer == null || !isPublic(customer) || Boolean.TRUE.equals(customer.getLockStatus())
                || Boolean.TRUE.equals(customer.getDealStatus()) || customer.getPoolEntryTime() == null
                || customer.getPoolEntryTime().isAfter(expireBefore)
                || customer.getPoolCycleCount() == null || customer.getPoolCycleCount() < minimumPoolCycles
                || isGarbageProtected(customer, now)) {
            return false;
        }
        putCustomerGarbage(customer, "公海滞留超过自动清理期限且达到入池次数阈值",
                CrmCustomerOwnerRecordSourceEnum.AUTO_GARBAGE.getSource(), now);
        return true;
    }

    private void putCustomerGarbage(CrmCustomerDO customer, String reason, String source, LocalDateTime now) {
        if (customerMapper.updatePublicToGarbage(customer.getId(), now, reason) == 0) {
            throw exception(CUSTOMER_GARBAGE_SOURCE_INVALID);
        }
        // Garbage is an administrator-only quarantine: remove every customer and contact team grant.
        permissionService.deletePermissionIfPresent(CrmBizTypeEnum.CRM_CUSTOMER.getType(), customer.getId());
        for (CrmContactDO contact : contactService.getContactListByCustomerId(customer.getId())) {
            permissionService.deletePermissionIfPresent(CrmBizTypeEnum.CRM_CONTACT.getType(), contact.getId());
        }
        customerOwnerRecordMapper.insert(new CrmCustomerOwnerRecordDO().setCustomerId(customer.getId())
                .setPreviousOwnerUserId(null).setNewOwnerUserId(null)
                .setType(CrmCustomerOwnerRecordTypeEnum.PUT_GARBAGE.getType())
                .setSource(source).setReason(reason));
    }

    private void validateGarbageProtection(CrmCustomerDO customer, LocalDateTime now) {
        if (Boolean.TRUE.equals(customer.getLockStatus())) {
            throw exception(CUSTOMER_LOCKED, customer.getName());
        }
        if (Boolean.TRUE.equals(customer.getDealStatus())) {
            throw exception(CUSTOMER_ALREADY_DEAL);
        }
        if (businessMapper.existsActiveByCustomerId(customer.getId())) {
            throw exception(CUSTOMER_POOL_ACTIVE_BUSINESS, customer.getName());
        }
        if (contractMapper.existsProtectedByCustomerId(customer.getId(),
                poolPolicyProperties.getCustomer().getProtectedContractAuditStatuses(), now)) {
            throw exception(CUSTOMER_POOL_ACTIVE_CONTRACT, customer.getName());
        }
    }

    private boolean isGarbageProtected(CrmCustomerDO customer, LocalDateTime now) {
        return businessMapper.existsActiveByCustomerId(customer.getId())
                || contractMapper.existsProtectedByCustomerId(customer.getId(),
                poolPolicyProperties.getCustomer().getProtectedContractAuditStatuses(), now);
    }

    private void validateGarbageAdmin(Long userId) {
        if (!authorizationService.isCrmAdmin(userId)) {
            throw exception(CUSTOMER_GARBAGE_ADMIN_REQUIRED);
        }
    }

    private static void validatePublicCustomer(CrmCustomerDO customer) {
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        if (!isPublic(customer)) {
            throw exception(CUSTOMER_GARBAGE_SOURCE_INVALID);
        }
    }

    private static void validateGarbageCustomer(CrmCustomerDO customer) {
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        if (!Integer.valueOf(CrmCustomerPoolStatusEnum.GARBAGE.getStatus()).equals(customer.getPoolStatus())
                || customer.getOwnerUserId() != null) {
            throw exception(CUSTOMER_GARBAGE_STATE_INVALID);
        }
    }

    private static boolean isPublic(CrmCustomerDO customer) {
        return Integer.valueOf(CrmCustomerPoolStatusEnum.PUBLIC.getStatus()).equals(customer.getPoolStatus())
                && customer.getOwnerUserId() == null;
    }

    private CrmCustomerGarbageServiceImpl getSelf() {
        return SpringUtil.getBean(getClass());
    }
}

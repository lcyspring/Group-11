package com.meession.etm.module.crm.service.customer;

import com.meession.etm.framework.common.exception.ServiceException;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmCustomerGarbageServiceImplTest {

    @Mock private CrmCustomerMapper customerMapper;
    @Mock private CrmCustomerOwnerRecordMapper customerOwnerRecordMapper;
    @Mock private CrmCustomerReferenceMapper customerReferenceMapper;
    @Mock private CrmBusinessMapper businessMapper;
    @Mock private CrmContractMapper contractMapper;
    @Mock private CrmContactService contactService;
    @Mock private CrmPermissionService permissionService;
    @Mock private CrmFollowUpRecordService followUpRecordService;
    @Mock private CrmAuthorizationService authorizationService;
    @Mock private CrmPoolPolicyProperties poolPolicyProperties;
    @Mock private CrmPoolTimeProvider poolTimeProvider;
    @InjectMocks private CrmCustomerGarbageServiceImpl service;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 15, 12, 0);

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void nonAdminCannotViewGarbagePage() {
        when(authorizationService.isCrmAdmin(7L)).thenReturn(false);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.getGarbagePage(new CrmCustomerGarbagePageReqVO(), 7L));

        assertEquals(CUSTOMER_GARBAGE_ADMIN_REQUIRED.getCode(), exception.getCode());
        verify(customerMapper, never()).selectGarbagePage(any());
    }

    @Test
    void manualPutQuarantinesCustomerAndContactPermissions() {
        CrmCustomerDO customer = publicCustomer();
        when(authorizationService.isCrmAdmin(1L)).thenReturn(true);
        when(customerMapper.selectByIdForUpdate(20L)).thenReturn(customer);
        when(poolTimeProvider.now()).thenReturn(NOW);
        when(businessMapper.existsActiveByCustomerId(20L)).thenReturn(false);
        when(poolPolicyProperties.getCustomer()).thenReturn(customerPolicy());
        when(contractMapper.existsProtectedByCustomerId(20L, List.of(0, 10, 20), NOW)).thenReturn(false);
        when(customerMapper.updatePublicToGarbage(20L, NOW, "无效联系方式")).thenReturn(1);
        when(contactService.getContactListByCustomerId(20L)).thenReturn(
                List.of(new CrmContactDO().setId(31L), new CrmContactDO().setId(32L)));

        service.putCustomerGarbage(new CrmCustomerGarbagePutReqVO()
                .setCustomerId(20L).setReason("  无效联系方式  "), 1L);

        verify(permissionService).deletePermissionIfPresent(CrmBizTypeEnum.CRM_CUSTOMER.getType(), 20L);
        verify(permissionService).deletePermissionIfPresent(CrmBizTypeEnum.CRM_CONTACT.getType(), 31L);
        verify(permissionService).deletePermissionIfPresent(CrmBizTypeEnum.CRM_CONTACT.getType(), 32L);
        ArgumentCaptor<CrmCustomerOwnerRecordDO> record = ArgumentCaptor.forClass(CrmCustomerOwnerRecordDO.class);
        verify(customerOwnerRecordMapper).insert(record.capture());
        assertEquals(CrmCustomerOwnerRecordTypeEnum.PUT_GARBAGE.getType(), record.getValue().getType());
        assertEquals(CrmCustomerOwnerRecordSourceEnum.MANUAL_GARBAGE.getSource(), record.getValue().getSource());
        assertEquals("无效联系方式", record.getValue().getReason());
    }

    @Test
    void manualPutRejectsActiveBusiness() {
        when(authorizationService.isCrmAdmin(1L)).thenReturn(true);
        when(customerMapper.selectByIdForUpdate(20L)).thenReturn(publicCustomer());
        when(poolTimeProvider.now()).thenReturn(NOW);
        when(businessMapper.existsActiveByCustomerId(20L)).thenReturn(true);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.putCustomerGarbage(
                new CrmCustomerGarbagePutReqVO().setCustomerId(20L).setReason("无效"), 1L));

        assertEquals(CUSTOMER_GARBAGE_ACTIVE_BUSINESS.getCode(), exception.getCode());
        assertEquals("客户【测试客户】存在进行中的商机，不能转入垃圾池", exception.getMessage());
        verify(customerMapper, never()).updatePublicToGarbage(any(), any(), any());
    }

    @Test
    void manualPutRejectsProtectedContractWithGarbageSpecificMessage() {
        when(authorizationService.isCrmAdmin(1L)).thenReturn(true);
        when(customerMapper.selectByIdForUpdate(20L)).thenReturn(publicCustomer());
        when(poolTimeProvider.now()).thenReturn(NOW);
        when(businessMapper.existsActiveByCustomerId(20L)).thenReturn(false);
        when(poolPolicyProperties.getCustomer()).thenReturn(customerPolicy());
        when(contractMapper.existsProtectedByCustomerId(20L, List.of(0, 10, 20), NOW)).thenReturn(true);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.putCustomerGarbage(
                new CrmCustomerGarbagePutReqVO().setCustomerId(20L).setReason("无效"), 1L));

        assertEquals(CUSTOMER_GARBAGE_ACTIVE_CONTRACT.getCode(), exception.getCode());
        assertEquals("客户【测试客户】存在未完结销售单据，不能转入垃圾池", exception.getMessage());
        verify(customerMapper, never()).updatePublicToGarbage(any(), any(), any());
    }

    @Test
    void restoreReturnsGarbageCustomerToPublicWithoutOwnerGrant() {
        when(authorizationService.isCrmAdmin(1L)).thenReturn(true);
        when(customerMapper.selectByIdForUpdate(20L)).thenReturn(garbageCustomer());
        when(poolTimeProvider.now()).thenReturn(NOW);
        when(customerMapper.updateGarbageToPublic(20L, NOW,
                CrmCustomerOwnerRecordSourceEnum.RESTORE_PUBLIC.getSource())).thenReturn(1);

        service.restoreCustomerToPublicPool(20L, 1L);

        ArgumentCaptor<CrmCustomerOwnerRecordDO> record = ArgumentCaptor.forClass(CrmCustomerOwnerRecordDO.class);
        verify(customerOwnerRecordMapper).insert(record.capture());
        assertEquals(CrmCustomerOwnerRecordTypeEnum.RESTORE_PUBLIC.getType(), record.getValue().getType());
        verify(permissionService, never()).replaceOwnerPermission(any(), any(), any());
    }

    @Test
    void permanentDeleteRejectsAnyDownstreamReference() {
        TenantContextHolder.setTenantId(9L);
        when(authorizationService.isCrmAdmin(1L)).thenReturn(true);
        when(customerMapper.selectByIdForUpdate(20L)).thenReturn(garbageCustomer());
        when(customerReferenceMapper.selectFirstReference(9L, 20L)).thenReturn("客服工单");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.permanentlyDeleteGarbageCustomer(20L, 1L));

        assertEquals(CUSTOMER_GARBAGE_DELETE_REFERENCED.getCode(), exception.getCode());
        verify(customerMapper, never()).deleteGarbagePermanently(any(), any());
    }

    @Test
    void permanentDeleteUsesStateConditionalPhysicalDelete() {
        TenantContextHolder.setTenantId(9L);
        when(authorizationService.isCrmAdmin(1L)).thenReturn(true);
        when(customerMapper.selectByIdForUpdate(20L)).thenReturn(garbageCustomer());
        when(customerReferenceMapper.selectFirstReference(9L, 20L)).thenReturn(null);
        when(customerMapper.deleteGarbagePermanently(9L, 20L)).thenReturn(1);

        service.permanentlyDeleteGarbageCustomer(20L, 1L);

        verify(followUpRecordService).deleteFollowUpRecordByBiz(CrmBizTypeEnum.CRM_CUSTOMER.getType(), 20L);
        verify(customerMapper).deleteGarbagePermanently(9L, 20L);
    }

    @Test
    void autoPutRechecksAgeCyclesAndProtectionUnderLock() {
        CrmCustomerDO customer = publicCustomer().setPoolEntryTime(NOW.minusDays(181)).setPoolCycleCount(3);
        when(customerMapper.selectByIdForUpdate(20L)).thenReturn(customer);
        when(businessMapper.existsActiveByCustomerId(20L)).thenReturn(false);
        when(poolPolicyProperties.getCustomer()).thenReturn(customerPolicy());
        when(contractMapper.existsProtectedByCustomerId(20L, List.of(0, 10, 20), NOW)).thenReturn(false);
        when(customerMapper.updatePublicToGarbage(20L, NOW,
                "公海滞留超过自动清理期限且达到入池次数阈值")).thenReturn(1);
        when(contactService.getContactListByCustomerId(20L)).thenReturn(List.of());

        boolean changed = service.autoPutSingleCustomerGarbage(20L, NOW.minusDays(180), 3, NOW);

        assertTrue(changed);
    }

    @Test
    void autoPutSkipsCustomerBelowCycleThreshold() {
        when(customerMapper.selectByIdForUpdate(20L)).thenReturn(
                publicCustomer().setPoolEntryTime(NOW.minusDays(181)).setPoolCycleCount(2));

        boolean changed = service.autoPutSingleCustomerGarbage(20L, NOW.minusDays(180), 3, NOW);

        assertFalse(changed);
        verify(customerMapper, never()).updatePublicToGarbage(any(), any(), any());
    }

    private static CrmCustomerDO publicCustomer() {
        return new CrmCustomerDO().setId(20L).setName("测试客户")
                .setPoolStatus(CrmCustomerPoolStatusEnum.PUBLIC.getStatus())
                .setOwnerUserId(null).setLockStatus(false).setDealStatus(false)
                .setPoolCycleCount(1).setPoolEntryTime(NOW.minusDays(10));
    }

    private static CrmCustomerDO garbageCustomer() {
        return publicCustomer().setPoolStatus(CrmCustomerPoolStatusEnum.GARBAGE.getStatus())
                .setGarbageTime(NOW.minusDays(1)).setGarbageReason("无效");
    }

    private static CrmPoolPolicyProperties.Customer customerPolicy() {
        CrmPoolPolicyProperties.Customer policy = new CrmPoolPolicyProperties.Customer();
        policy.setProtectedContractAuditStatuses(List.of(0, 10, 20));
        return policy;
    }
}

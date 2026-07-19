package com.meession.etm.module.crm.service.receivable;

import com.meession.etm.framework.common.exception.ServiceException;
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
import com.meession.etm.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.RECEIVABLE_PLAN_CREATE_FAIL_CONTRACT_NOT_APPROVE;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.RECEIVABLE_PLAN_DELETE_FAIL_LINKED;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.RECEIVABLE_PLAN_EXISTS_RECEIVABLE;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.RECEIVABLE_PLAN_PRICE_EXCEEDS_CONTRACT;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.RECEIVABLE_PLAN_UPDATE_FAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class CrmReceivablePlanServiceImplTest {

    @Mock
    private CrmReceivablePlanMapper receivablePlanMapper;
    @Mock
    private CrmContractService contractService;
    @Mock
    private CrmPermissionService permissionService;
    @Mock
    private AdminUserApi adminUserApi;

    @InjectMocks
    private CrmReceivablePlanServiceImpl service;

    @Test
    void createReceivablePlanRejectsUnapprovedContract() {
        when(contractService.validateContract(20L)).thenReturn(new CrmContractDO()
                .setId(20L).setAuditStatus(CrmAuditStatusEnum.PROCESS.getStatus()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createReceivablePlan(request()));

        assertEquals(RECEIVABLE_PLAN_CREATE_FAIL_CONTRACT_NOT_APPROVE.getCode(), exception.getCode());
        verify(receivablePlanMapper, never()).insert(any(CrmReceivablePlanDO.class));
    }

    @Test
    void createReceivablePlanRequiresContractWritePermission() throws NoSuchMethodException {
        CrmPermission permission = CrmReceivablePlanServiceImpl.class
                .getMethod("createReceivablePlan", CrmReceivablePlanSaveReqVO.class)
                .getAnnotation(CrmPermission.class);

        assertEquals(CrmBizTypeEnum.CRM_CONTRACT, permission.bizType()[0]);
        assertEquals("#createReqVO.contractId", permission.bizId());
        assertEquals(CrmPermissionLevelEnum.WRITE, permission.level());
    }

    @Test
    void createReceivablePlanAcceptsApprovedContract() {
        when(contractService.validateContract(20L)).thenReturn(new CrmContractDO()
                .setId(20L).setCustomerId(30L).setTotalPrice(new BigDecimal("100.00"))
                .setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus()));
        when(receivablePlanMapper.selectListByContractId(20L)).thenReturn(List.of());
        doAnswer(invocation -> {
            ((CrmReceivablePlanDO) invocation.getArgument(0)).setId(70L);
            return 1;
        }).when(receivablePlanMapper).insert(any(CrmReceivablePlanDO.class));

        assertEquals(70L, service.createReceivablePlan(request()));

        ArgumentCaptor<CrmReceivablePlanDO> captor = ArgumentCaptor.forClass(CrmReceivablePlanDO.class);
        verify(receivablePlanMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getPeriod());
        assertEquals(30L, captor.getValue().getCustomerId());
    }

    @Test
    void createReceivablePlanRejectsTotalAboveContract() {
        when(contractService.validateContract(20L)).thenReturn(new CrmContractDO()
                .setId(20L).setCustomerId(30L).setTotalPrice(new BigDecimal("100.00"))
                .setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus()));
        when(receivablePlanMapper.selectListByContractId(20L)).thenReturn(List.of(
                new CrmReceivablePlanDO().setId(60L).setPrice(new BigDecimal("60.00"))));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createReceivablePlan(request()));

        assertEquals(RECEIVABLE_PLAN_PRICE_EXCEEDS_CONTRACT.getCode(), exception.getCode());
        verify(receivablePlanMapper, never()).insert(any(CrmReceivablePlanDO.class));
    }

    @Test
    void deleteReceivablePlanRejectsLinkedReceivable() {
        when(receivablePlanMapper.selectByIdForUpdate(70L)).thenReturn(
                new CrmReceivablePlanDO().setId(70L).setReceivableId(80L));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.deleteReceivablePlan(70L));

        assertEquals(RECEIVABLE_PLAN_DELETE_FAIL_LINKED.getCode(), exception.getCode());
        verify(receivablePlanMapper, never()).deleteById(70L);
    }

    @Test
    void linkReceivableRejectsConcurrentDuplicate() {
        when(receivablePlanMapper.selectByIdForUpdate(70L)).thenReturn(
                new CrmReceivablePlanDO().setId(70L).setReceivableId(80L));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.updateReceivablePlanReceivableId(70L, 81L));

        assertEquals(RECEIVABLE_PLAN_EXISTS_RECEIVABLE.getCode(), exception.getCode());
        verify(receivablePlanMapper, never()).updateById(any(CrmReceivablePlanDO.class));
    }

    @Test
    void unlinkReceivablePlanClearsMatchingRelation() {
        when(receivablePlanMapper.selectByIdForUpdate(70L)).thenReturn(
                new CrmReceivablePlanDO().setId(70L).setReceivableId(80L));
        when(receivablePlanMapper.clearReceivableIdIfMatches(70L, 80L)).thenReturn(1);

        service.unlinkReceivablePlan(70L, 80L);

        verify(receivablePlanMapper).clearReceivableIdIfMatches(70L, 80L);
    }

    @Test
    void unlinkReceivablePlanRejectsChangedRelation() {
        when(receivablePlanMapper.selectByIdForUpdate(70L)).thenReturn(
                new CrmReceivablePlanDO().setId(70L).setReceivableId(81L));
        when(receivablePlanMapper.clearReceivableIdIfMatches(70L, 80L)).thenReturn(0);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.unlinkReceivablePlan(70L, 80L));

        assertEquals(RECEIVABLE_PLAN_UPDATE_FAIL.getCode(), exception.getCode());
    }

    private static CrmReceivablePlanSaveReqVO request() {
        return new CrmReceivablePlanSaveReqVO()
                .setContractId(20L)
                .setOwnerUserId(1L)
                .setPrice(new BigDecimal("50.00"))
                .setReturnTime(LocalDateTime.of(2026, 7, 14, 0, 0));
    }

}

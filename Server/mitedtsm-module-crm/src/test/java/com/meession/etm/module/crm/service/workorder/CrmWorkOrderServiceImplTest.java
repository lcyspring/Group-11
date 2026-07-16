package com.meession.etm.module.crm.service.workorder;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.crm.controller.admin.workorder.vo.*;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderCcDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderGroupDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderRecordDO;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderCcMapper;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderMapper;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderRecordMapper;
import com.meession.etm.module.crm.dal.redis.no.CrmNoRedisDAO;
import com.meession.etm.module.crm.enums.workorder.CrmWorkOrderStatusEnum;
import com.meession.etm.module.crm.framework.workorder.CrmWorkOrderDispatchProperties;
import com.meession.etm.module.crm.service.business.CrmBusinessService;
import com.meession.etm.module.crm.service.contract.CrmContractService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmWorkOrderServiceImplTest {

    @Mock private CrmWorkOrderMapper workOrderMapper;
    @Mock private CrmWorkOrderRecordMapper recordMapper;
    @Mock private CrmWorkOrderCcMapper ccMapper;
    @Mock private CrmNoRedisDAO noRedisDAO;
    @Mock private CrmCustomerService customerService;
    @Mock private CrmBusinessService businessService;
    @Mock private CrmContractService contractService;
    @Mock private AdminUserApi adminUserApi;
    @Mock private CrmWorkOrderNotificationService notificationService;
    @Mock private CrmWorkOrderGroupService groupService;
    @Mock private CrmWorkOrderDispatchProperties dispatchProperties;
    @InjectMocks private CrmWorkOrderServiceImpl service;

    @BeforeEach
    void setUpDispatchPolicy() {
        lenient().when(dispatchProperties.isEnabled()).thenReturn(true);
        lenient().when(dispatchProperties.isAutoAssignOnCreate()).thenReturn(true);
        lenient().when(dispatchProperties.getFallbackMode()).thenReturn(
                CrmWorkOrderDispatchProperties.FallbackMode.UNASSIGNED_POOL);
        lenient().when(dispatchProperties.getMaxCcUsers()).thenReturn(20);
        lenient().when(dispatchProperties.getDescriptionMinLength()).thenReturn(20);
        lenient().when(dispatchProperties.getSolutionMinLength()).thenReturn(20);
        lenient().when(groupService.validateEnabledGroup(5L, 1)).thenReturn(group());
        lenient().when(groupService.getOrderedMemberUserIds(5L)).thenReturn(List.of(2L, 3L));
        lenient().when(groupService.isGroupManager(5L, 1L)).thenReturn(true);
    }

    @Test
    void createWritesInitialRecordAndNotifiesHandler() {
        when(noRedisDAO.generateMonthly(CrmNoRedisDAO.WORK_ORDER_PREFIX)).thenReturn("W-202607-0001");
        when(workOrderMapper.selectByNo("W-202607-0001")).thenReturn(null);
        doAnswer(invocation -> {
            ((CrmWorkOrderDO) invocation.getArgument(0)).setId(10L);
            return 1;
        }).when(workOrderMapper).insert(Mockito.<CrmWorkOrderDO>any());

        Long id = service.createWorkOrder(request(2L), 1L, true, false);

        assertEquals(10L, id);
        ArgumentCaptor<CrmWorkOrderDO> orderCaptor = ArgumentCaptor.forClass(CrmWorkOrderDO.class);
        verify(workOrderMapper).insert(orderCaptor.capture());
        assertEquals("1", orderCaptor.getValue().getCreator());
        assertEquals(CrmWorkOrderStatusEnum.PENDING.getStatus(), orderCaptor.getValue().getStatus());
        verify(recordMapper).insert(any(com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderRecordDO.class));
        verify(notificationService).notifyAssigned(orderCaptor.getValue());
    }

    @Test
    void handlerCanStartPendingOrderAndAtomicTransitionIsUsed() {
        CrmWorkOrderDO order = order(10L, "1", 2L, CrmWorkOrderStatusEnum.PENDING.getStatus());
        when(workOrderMapper.selectById(10L)).thenReturn(order);
        when(workOrderMapper.startIfPending(eq(10L), anyInt(), anyInt(), any())).thenReturn(1);

        service.startWorkOrder(new CrmWorkOrderActionReqVO().setId(10L).setRemark("开始"), 2L);

        verify(workOrderMapper).startIfPending(eq(10L), eq(10), eq(20), any());
        ArgumentCaptor<com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderRecordDO> record =
                ArgumentCaptor.forClass(com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderRecordDO.class);
        verify(recordMapper).insert(record.capture());
        assertEquals(3, record.getValue().getActionType());
        assertEquals(20, record.getValue().getToStatus());
    }

    @Test
    void nonHandlerCannotStartOrder() {
        when(workOrderMapper.selectById(10L)).thenReturn(order(10L, "3", 2L, 10));
        assertServiceException(() -> service.startWorkOrder(new CrmWorkOrderActionReqVO().setId(10L), 3L),
                WORK_ORDER_HANDLER_ONLY);
        verify(workOrderMapper, never()).startIfPending(any(), anyInt(), anyInt(), any());
    }

    @Test
    void completionRequiresSolution() {
        when(workOrderMapper.selectById(10L)).thenReturn(order(10L, "1", 2L, 20));
        ServiceException error = org.junit.jupiter.api.Assertions.assertThrows(ServiceException.class,
                () -> service.completeWorkOrder(new CrmWorkOrderCompleteReqVO().setId(10L).setSolution(" "), 2L));
        assertEquals(WORK_ORDER_SOLUTION_REQUIRED.getCode(), error.getCode());
        verify(workOrderMapper, never()).completeIfProcessing(any(), anyInt(), anyInt(), any(), any());
    }

    @Test
    void returnWritesReasonAndNotifiesCreator() {
        CrmWorkOrderDO order = order(10L, "1", 2L, 20);
        when(workOrderMapper.selectById(10L)).thenReturn(order);
        when(workOrderMapper.returnIfProcessing(10L, 20, 40, "缺少现场照片")).thenReturn(1);

        service.returnWorkOrder(new CrmWorkOrderReturnReqVO().setId(10L).setReason("缺少现场照片"), 2L);

        verify(notificationService).notifyReturned(argThat(item ->
                item.getStatus().equals(40) && "缺少现场照片".equals(item.getReturnReason())));
    }

    @Test
    void creatorCanAssignPendingOrderWithAtomicOldHandlerGuard() {
        CrmWorkOrderDO order = order(10L, "1", 2L, 10);
        when(workOrderMapper.selectById(10L)).thenReturn(order);
        when(workOrderMapper.assignIfPending(eq(10L), eq(10), eq(2L), eq(5L), eq(5L), eq(3L), eq(4), any()))
                .thenReturn(1);

        service.assignWorkOrder(new CrmWorkOrderAssignReqVO().setId(10L).setHandlerUserId(3L)
                .setRemark("转客服二组"), 1L, false);

        verify(adminUserApi).validateUser(3L);
        verify(workOrderMapper).assignIfPending(eq(10L), eq(10), eq(2L), eq(5L), eq(5L), eq(3L), eq(4), any());
        verify(recordMapper).insert(org.mockito.ArgumentMatchers.<CrmWorkOrderRecordDO>argThat(record ->
                record.getActionType().equals(7)
                && record.getHandlerUserId().equals(3L) && "转客服二组".equals(record.getRemark())));
        verify(notificationService).notifyAssigned(argThat(item -> item.getHandlerUserId().equals(3L)));
    }

    @Test
    void handlerCannotAssignWithoutQueryAllScope() {
        when(workOrderMapper.selectById(10L)).thenReturn(order(10L, "1", 2L, 10));

        assertServiceException(() -> service.assignWorkOrder(
                new CrmWorkOrderAssignReqVO().setId(10L).setHandlerUserId(3L), 2L, false),
                WORK_ORDER_ASSIGN_DENIED);
        verify(workOrderMapper, never()).assignIfPending(any(), anyInt(), any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    void assignmentRejectsUnchangedHandler() {
        when(workOrderMapper.selectById(10L)).thenReturn(order(10L, "1", 2L, 10));

        assertServiceException(() -> service.assignWorkOrder(
                new CrmWorkOrderAssignReqVO().setId(10L).setHandlerUserId(2L), 1L, false),
                WORK_ORDER_HANDLER_UNCHANGED);
        verify(adminUserApi, never()).validateUser(anyLong());
    }

    @Test
    void sourceCustomerMismatchIsRejectedBeforeInsert() {
        when(businessService.validateBusiness(9L)).thenReturn(new CrmBusinessDO().setId(9L).setCustomerId(8L));
        CrmWorkOrderSaveReqVO request = request(2L).setCustomerId(7L).setSourceType(1).setSourceId(9L);

        assertServiceException(() -> service.createWorkOrder(request, 1L, true, false), WORK_ORDER_SOURCE_CUSTOMER_MISMATCH);
        verify(workOrderMapper, never()).insert(Mockito.<CrmWorkOrderDO>any());
    }

    @Test
    void automaticDispatchChoosesLeastLoadedGroupMember() {
        when(noRedisDAO.generateMonthly(CrmNoRedisDAO.WORK_ORDER_PREFIX)).thenReturn("W-202607-0002");
        when(workOrderMapper.countOpenByHandler(2L)).thenReturn(4);
        when(workOrderMapper.countOpenByHandler(3L)).thenReturn(1);
        doAnswer(invocation -> { ((CrmWorkOrderDO) invocation.getArgument(0)).setId(11L); return 1; })
                .when(workOrderMapper).insert(Mockito.<CrmWorkOrderDO>any());

        service.createWorkOrder(request(null), 1L, false, false);

        verify(workOrderMapper).insert(argThat((CrmWorkOrderDO order) -> order.getHandlerUserId().equals(3L)
                && order.getGroupId().equals(5L) && order.getDispatchMode().equals(2)));
    }

    @Test
    void groupMemberClaimsUnassignedOrderAtomically() {
        CrmWorkOrderDO order = order(12L, "1", null, 10);
        when(workOrderMapper.selectById(12L)).thenReturn(order);
        when(groupService.isGroupMember(5L, 3L)).thenReturn(true);
        when(workOrderMapper.claimIfUnassigned(eq(12L), eq(5L), eq(3L), any())).thenReturn(1);

        service.claimWorkOrder(new CrmWorkOrderActionReqVO().setId(12L).setRemark("主动领取"), 3L);

        verify(recordMapper).insert(argThat((CrmWorkOrderRecordDO record) -> record.getActionType().equals(8)
                && record.getHandlerUserId().equals(3L)));
        verify(notificationService).notifyAssigned(argThat(item -> item.getHandlerUserId().equals(3L)));
    }

    @Test
    void copiedUsersArePersistedAndNotified() {
        when(noRedisDAO.generateMonthly(CrmNoRedisDAO.WORK_ORDER_PREFIX)).thenReturn("W-202607-0003");
        doAnswer(invocation -> { ((CrmWorkOrderDO) invocation.getArgument(0)).setId(13L); return 1; })
                .when(workOrderMapper).insert(Mockito.<CrmWorkOrderDO>any());
        CrmWorkOrderSaveReqVO request = request(2L).setCcUserIds(List.of(8L, 8L, 9L));

        service.createWorkOrder(request, 1L, true, false);

        verify(ccMapper, times(2)).insert(any(CrmWorkOrderCcDO.class));
        verify(notificationService).notifyCopied(any(CrmWorkOrderDO.class), eq(new java.util.LinkedHashSet<>(List.of(8L, 9L))));
    }

    @Test
    void automaticDispatchUsesStableUserIdTieBreak() {
        when(noRedisDAO.generateMonthly(CrmNoRedisDAO.WORK_ORDER_PREFIX)).thenReturn("W-202607-0004");
        when(workOrderMapper.countOpenByHandler(2L)).thenReturn(1);
        when(workOrderMapper.countOpenByHandler(3L)).thenReturn(1);
        doAnswer(invocation -> { ((CrmWorkOrderDO) invocation.getArgument(0)).setId(14L); return 1; })
                .when(workOrderMapper).insert(Mockito.<CrmWorkOrderDO>any());

        service.createWorkOrder(request(null), 1L, false, false);

        verify(workOrderMapper).insert(argThat((CrmWorkOrderDO order) -> order.getHandlerUserId().equals(2L)
                && order.getDispatchMode().equals(2)));
    }

    @Test
    void noAutomaticCandidateFallsBackToGroupUnassignedPool() {
        CrmWorkOrderGroupDO emptyGroup = new CrmWorkOrderGroupDO().setId(6L).setManagerUserId(4L)
                .setSupportedTypes(List.of(1)).setStatus(0).setSort(2);
        when(groupService.validateEnabledGroup(6L, 1)).thenReturn(emptyGroup);
        when(groupService.getOrderedMemberUserIds(6L)).thenReturn(List.of());
        when(noRedisDAO.generateMonthly(CrmNoRedisDAO.WORK_ORDER_PREFIX)).thenReturn("W-202607-0005");
        doAnswer(invocation -> { ((CrmWorkOrderDO) invocation.getArgument(0)).setId(15L); return 1; })
                .when(workOrderMapper).insert(Mockito.<CrmWorkOrderDO>any());

        service.createWorkOrder(request(null).setGroupId(6L), 1L, false, false);

        verify(workOrderMapper).insert(argThat((CrmWorkOrderDO order) -> order.getGroupId().equals(6L)
                && order.getHandlerUserId() == null && order.getDispatchMode().equals(0)
                && order.getAssignTime() == null));
        verify(notificationService).notifyAssigned(argThat(item -> item.getHandlerUserId() == null));
    }

    @Test
    void nonMemberCannotClaimUnassignedGroupOrder() {
        when(workOrderMapper.selectById(16L)).thenReturn(order(16L, "1", null, 10));
        when(groupService.isGroupMember(5L, 9L)).thenReturn(false);

        assertServiceException(() -> service.claimWorkOrder(new CrmWorkOrderActionReqVO().setId(16L), 9L),
                WORK_ORDER_CLAIM_DENIED);
        verify(workOrderMapper, never()).claimIfUnassigned(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void concurrentClaimLosesAtomicGuard() {
        when(workOrderMapper.selectById(17L)).thenReturn(order(17L, "1", null, 10));
        when(groupService.isGroupMember(5L, 3L)).thenReturn(true);
        when(workOrderMapper.claimIfUnassigned(eq(17L), eq(5L), eq(3L), any())).thenReturn(0);

        assertServiceException(() -> service.claimWorkOrder(new CrmWorkOrderActionReqVO().setId(17L), 3L),
                WORK_ORDER_STATUS_TRANSITION_INVALID);
        verify(recordMapper, never()).insert(any(CrmWorkOrderRecordDO.class));
    }

    @Test
    void groupManagerCanReadAssignedGroupOrder() {
        when(workOrderMapper.selectById(18L)).thenReturn(order(18L, "7", 2L, 10));
        when(groupService.isGroupManager(5L, 6L)).thenReturn(true);

        assertEquals(18L, service.getWorkOrder(18L, 6L, false).getId());
    }

    @Test
    void copiedUserCanReadButCannotProcessOrder() {
        when(workOrderMapper.selectById(19L)).thenReturn(order(19L, "7", 2L, 10));
        when(ccMapper.exists(19L, 8L)).thenReturn(true);

        assertEquals(19L, service.getWorkOrder(19L, 8L, false).getId());
        assertServiceException(() -> service.startWorkOrder(new CrmWorkOrderActionReqVO().setId(19L), 8L),
                WORK_ORDER_HANDLER_ONLY);
    }

    @Test
    void ordinaryMemberCannotReadAnotherHandlersAssignedOrder() {
        when(workOrderMapper.selectById(20L)).thenReturn(order(20L, "7", 2L, 10));

        assertServiceException(() -> service.getWorkOrder(20L, 3L, false), WORK_ORDER_QUERY_DENIED);
    }

    @Test
    void groupManagerCannotAssignIntoAnotherGroupWithoutAssignAll() {
        when(workOrderMapper.selectById(21L)).thenReturn(order(21L, "7", 2L, 10));

        assertServiceException(() -> service.assignWorkOrder(new CrmWorkOrderAssignReqVO().setId(21L)
                .setGroupId(6L).setHandlerUserId(4L), 1L, false), WORK_ORDER_ASSIGN_DENIED);
        verify(workOrderMapper, never()).assignIfPending(any(), anyInt(), any(), any(), any(), any(), anyInt(), any());
    }

    private CrmWorkOrderSaveReqVO request(Long handlerId) {
        return new CrmWorkOrderSaveReqVO().setTitle("服务问题").setType(1).setPriority(2)
                .setCustomerId(7L).setSourceType(0).setGroupId(5L).setHandlerUserId(handlerId)
                .setDescription("客户反馈的问题已经持续多日，需要客服尽快跟进处理。")
                .setAttachmentUrls(List.of("https://files.example/a.png"));
    }

    private CrmWorkOrderDO order(Long id, String creator, Long handler, Integer status) {
        CrmWorkOrderDO order = new CrmWorkOrderDO().setId(id).setHandlerUserId(handler)
                .setCustomerId(7L).setSourceType(0).setGroupId(5L).setStatus(status).setTitle("服务问题");
        order.setCreator(creator);
        return order;
    }

    private CrmWorkOrderGroupDO group() {
        return new CrmWorkOrderGroupDO().setId(5L).setName("客服一组").setManagerUserId(1L)
                .setSupportedTypes(List.of(1)).setStatus(0).setSort(1);
    }
}

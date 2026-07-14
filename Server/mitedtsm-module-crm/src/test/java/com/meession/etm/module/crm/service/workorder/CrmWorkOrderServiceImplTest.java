package com.meession.etm.module.crm.service.workorder;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.crm.controller.admin.workorder.vo.*;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderDO;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderMapper;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderRecordMapper;
import com.meession.etm.module.crm.dal.redis.no.CrmNoRedisDAO;
import com.meession.etm.module.crm.enums.workorder.CrmWorkOrderStatusEnum;
import com.meession.etm.module.crm.service.business.CrmBusinessService;
import com.meession.etm.module.crm.service.contract.CrmContractService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    @Mock private CrmNoRedisDAO noRedisDAO;
    @Mock private CrmCustomerService customerService;
    @Mock private CrmBusinessService businessService;
    @Mock private CrmContractService contractService;
    @Mock private AdminUserApi adminUserApi;
    @Mock private CrmWorkOrderNotificationService notificationService;
    @InjectMocks private CrmWorkOrderServiceImpl service;

    @Test
    void createWritesInitialRecordAndNotifiesHandler() {
        when(noRedisDAO.generateMonthly(CrmNoRedisDAO.WORK_ORDER_PREFIX)).thenReturn("W-202607-0001");
        when(workOrderMapper.selectByNo("W-202607-0001")).thenReturn(null);
        doAnswer(invocation -> {
            ((CrmWorkOrderDO) invocation.getArgument(0)).setId(10L);
            return 1;
        }).when(workOrderMapper).insert(any(CrmWorkOrderDO.class));

        Long id = service.createWorkOrder(request(2L), 1L);

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
    void sourceCustomerMismatchIsRejectedBeforeInsert() {
        when(businessService.validateBusiness(9L)).thenReturn(new CrmBusinessDO().setId(9L).setCustomerId(8L));
        CrmWorkOrderSaveReqVO request = request(2L).setCustomerId(7L).setSourceType(1).setSourceId(9L);

        assertServiceException(() -> service.createWorkOrder(request, 1L), WORK_ORDER_SOURCE_CUSTOMER_MISMATCH);
        verify(workOrderMapper, never()).insert(any(CrmWorkOrderDO.class));
    }

    private CrmWorkOrderSaveReqVO request(Long handlerId) {
        return new CrmWorkOrderSaveReqVO().setTitle("服务问题").setType(1).setPriority(2)
                .setCustomerId(7L).setSourceType(0).setHandlerUserId(handlerId)
                .setDescription("客户反馈问题").setAttachmentUrls(List.of("https://files.example/a.png"));
    }

    private CrmWorkOrderDO order(Long id, String creator, Long handler, Integer status) {
        CrmWorkOrderDO order = new CrmWorkOrderDO().setId(id).setHandlerUserId(handler)
                .setCustomerId(7L).setSourceType(0).setStatus(status).setTitle("服务问题");
        order.setCreator(creator);
        return order;
    }
}

package com.meession.etm.module.crm.service.visit;

import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import com.meession.etm.module.crm.controller.admin.followup.vo.CrmFollowUpRecordSaveReqVO;
import com.meession.etm.module.crm.controller.admin.visit.vo.CrmCustomerVisitCreateReqVO;
import com.meession.etm.module.crm.controller.admin.visit.vo.CrmCustomerVisitResultReqVO;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.visit.CrmCustomerVisitDO;
import com.meession.etm.module.crm.dal.mysql.visit.CrmCustomerVisitMapper;
import com.meession.etm.module.crm.service.contact.CrmContactService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.followup.CrmFollowUpRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmCustomerVisitServiceImplTest {
    @Mock private CrmCustomerVisitMapper visitMapper;
    @Mock private CrmCustomerService customerService;
    @Mock private CrmContactService contactService;
    @Mock private CrmFollowUpRecordService followUpRecordService;
    @Mock private BpmProcessInstanceApi processInstanceApi;
    @InjectMocks private CrmCustomerVisitServiceImpl service;

    @Test
    void createValidatesContactAndStartsManagedProcess() {
        when(contactService.getContact(8L)).thenReturn(new CrmContactDO().setId(8L).setCustomerId(6L));
        doAnswer(invocation -> { ((CrmCustomerVisitDO) invocation.getArgument(0)).setId(9L); return 1; })
                .when(visitMapper).insert(any(CrmCustomerVisitDO.class));
        when(processInstanceApi.createProcessInstance(eq(7L), any())).thenReturn("process-1");

        assertEquals(9L, service.createVisit(7L, createRequest()));
        verify(customerService).validateCustomer(6L);
        verify(processInstanceApi).createProcessInstance(eq(7L), argThat(request ->
                request.getProcessDefinitionKey().equals("crm_customer_visit_audit")
                        && request.getBusinessKey().equals("9")));
        verify(visitMapper).updateById(org.mockito.ArgumentMatchers.<CrmCustomerVisitDO>argThat(
                update -> "process-1".equals(update.getProcessInstanceId())));
    }

    @Test
    void createRejectsContactFromAnotherCustomer() {
        when(contactService.getContact(8L)).thenReturn(new CrmContactDO().setId(8L).setCustomerId(99L));
        assertServiceException(() -> service.createVisit(7L, createRequest()), CUSTOMER_VISIT_CONTACT_MISMATCH);
        verify(visitMapper, never()).insert(any(CrmCustomerVisitDO.class));
    }

    @Test
    void recordResultCreatesVisitFollowUpAndLinksItOnce() {
        CrmCustomerVisitDO visit = approvedVisit();
        when(visitMapper.selectByIdForUpdate(9L)).thenReturn(visit);
        when(followUpRecordService.createFollowUpRecord(any())).thenReturn(44L);
        CrmCustomerVisitResultReqVO request = resultRequest();

        assertEquals(44L, service.recordResult(7L, request));
        ArgumentCaptor<CrmFollowUpRecordSaveReqVO> followUp = ArgumentCaptor.forClass(CrmFollowUpRecordSaveReqVO.class);
        verify(followUpRecordService).createFollowUpRecord(followUp.capture());
        assertEquals(3, followUp.getValue().getType());
        assertEquals(6L, followUp.getValue().getBizId());
        assertEquals(List.of(8L), followUp.getValue().getContactIds());
        verify(visitMapper).updateById(org.mockito.ArgumentMatchers.<CrmCustomerVisitDO>argThat(
                update -> update.getId().equals(9L) && update.getResultStatus().equals(1)
                        && update.getFollowUpRecordId().equals(44L)));
    }

    @Test
    void recordResultRejectsNonApprovedOrDuplicateVisit() {
        when(visitMapper.selectByIdForUpdate(9L)).thenReturn(approvedVisit().setAuditStatus(BpmTaskStatusEnum.RUNNING.getStatus()));
        assertServiceException(() -> service.recordResult(7L, resultRequest()), CUSTOMER_VISIT_RESULT_STATUS_INVALID);
        when(visitMapper.selectByIdForUpdate(9L)).thenReturn(approvedVisit().setFollowUpRecordId(44L));
        assertServiceException(() -> service.recordResult(7L, resultRequest()), CUSTOMER_VISIT_RESULT_ALREADY_RECORDED);
        verify(followUpRecordService, never()).createFollowUpRecord(any());
    }

    private static CrmCustomerVisitCreateReqVO createRequest() {
        return new CrmCustomerVisitCreateReqVO().setCustomerId(6L).setContactId(8L)
                .setPlannedStartTime(LocalDateTime.now().plusDays(1))
                .setPlannedEndTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .setLocation("客户总部会议室").setPurpose("沟通项目实施计划和下一阶段商务安排")
                .setParticipantUserIds(List.of(7L, 7L, 10L));
    }

    private static CrmCustomerVisitResultReqVO resultRequest() {
        return new CrmCustomerVisitResultReqVO().setId(9L)
                .setActualStartTime(LocalDateTime.now().minusHours(2))
                .setActualEndTime(LocalDateTime.now().minusHours(1))
                .setResultContent("确认项目范围并约定下一次技术方案评审")
                .setNextContactTime(LocalDateTime.now().plusDays(3));
    }

    private static CrmCustomerVisitDO approvedVisit() {
        return new CrmCustomerVisitDO().setId(9L).setApplicantUserId(7L).setCustomerId(6L).setContactId(8L)
                .setAuditStatus(BpmTaskStatusEnum.APPROVE.getStatus()).setResultStatus(0);
    }
}

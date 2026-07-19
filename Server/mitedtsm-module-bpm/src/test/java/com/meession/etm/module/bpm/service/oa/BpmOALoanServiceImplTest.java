package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALoanCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALoanRepaymentCreateReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOALoanDO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOALoanRepaymentDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOALoanMapper;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOALoanRepaymentMapper;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import com.meession.etm.module.bpm.framework.oa.BpmOALoanProperties;
import com.meession.etm.module.system.api.dept.PostApi;
import com.meession.etm.module.system.api.dept.dto.PostRespDTO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BpmOALoanServiceImplTest {

    @Test
    void blocksNewApplicationWhenOutstandingLoanExists() {
        Fixture f = fixture();
        when(f.loanMapper.selectOutstandingForUpdate(7L)).thenReturn(List.of(
                new BpmOALoanDO().setOutstandingAmount(new BigDecimal("321.50"))));
        assertThrows(ServiceException.class, () -> f.service.createLoan(7L, request("100")));
        verify(f.loanMapper, never()).insert(any(BpmOALoanDO.class));
    }

    @Test
    void managerOverLimitIsAllowedAndEscalatedIntoProcessVariables() {
        Fixture f = fixture();
        when(f.loanMapper.selectOutstandingForUpdate(7L)).thenReturn(List.of());
        when(f.userApi.getUser(7L)).thenReturn(new AdminUserRespDTO().setId(7L).setPostIds(Set.of(2L)));
        when(f.postApi.getPostList(Set.of(2L))).thenReturn(List.of(new PostRespDTO().setId(2L).setCode("manager")));
        doAnswer(invocation -> { invocation.<BpmOALoanDO>getArgument(0).setId(9L); return 1; })
                .when(f.loanMapper).insert(any(BpmOALoanDO.class));
        when(f.processApi.createProcessInstance(any(), any())).thenReturn("loan-process-9");

        assertEquals(9L, f.service.createLoan(7L, request("25000")));
        ArgumentCaptor<BpmOALoanDO> loan = ArgumentCaptor.forClass(BpmOALoanDO.class);
        verify(f.loanMapper).insert(loan.capture());
        assertEquals("manager", loan.getValue().getEmployeeLevel());
        assertTrue(loan.getValue().getEscalatedApproval());
        ArgumentCaptor<BpmProcessInstanceCreateReqDTO> process = ArgumentCaptor.forClass(BpmProcessInstanceCreateReqDTO.class);
        verify(f.processApi).createProcessInstance(eq(7L), process.capture());
        assertEquals(Boolean.TRUE, process.getValue().getVariables().get("escalatedApproval"));
    }

    @Test
    void approvalCreatesOutstandingBalanceExactlyOnce() {
        Fixture f = fixture();
        BpmOALoanDO running = new BpmOALoanDO().setId(4L).setStatus(BpmTaskStatusEnum.RUNNING.getStatus())
                .setAmount(new BigDecimal("5000"));
        BpmOALoanDO approved = new BpmOALoanDO().setId(4L).setStatus(BpmTaskStatusEnum.APPROVE.getStatus())
                .setAmount(new BigDecimal("5000"));
        when(f.loanMapper.selectByIdForUpdate(4L)).thenReturn(running, approved);
        f.service.updateLoanStatus(4L, BpmTaskStatusEnum.APPROVE.getStatus());
        f.service.updateLoanStatus(4L, BpmTaskStatusEnum.APPROVE.getStatus());
        ArgumentCaptor<BpmOALoanDO> update = ArgumentCaptor.forClass(BpmOALoanDO.class);
        verify(f.loanMapper, times(1)).updateById(update.capture());
        assertEquals(new BigDecimal("5000"), update.getValue().getOutstandingAmount());
        assertEquals(1, update.getValue().getRepaymentStatus());
    }

    @Test
    void partialThenFullRepaymentMaintainsLedgerAndClosesLoan() {
        Fixture f = fixture();
        BpmOALoanDO loan = new BpmOALoanDO().setId(6L).setUserId(7L)
                .setStatus(BpmTaskStatusEnum.APPROVE.getStatus()).setRepaymentStatus(1)
                .setOutstandingAmount(new BigDecimal("1000"));
        when(f.loanMapper.selectByIdForUpdate(6L)).thenReturn(loan);
        doAnswer(invocation -> { invocation.<BpmOALoanRepaymentDO>getArgument(0).setId(20L); return 1; })
                .when(f.repaymentMapper).insert(any(BpmOALoanRepaymentDO.class));
        BpmOALoanRepaymentCreateReqVO repayment = new BpmOALoanRepaymentCreateReqVO()
                .setLoanId(6L).setAmount(new BigDecimal("1000"));
        assertEquals(20L, f.service.createRepayment(7L, repayment));
        ArgumentCaptor<BpmOALoanDO> update = ArgumentCaptor.forClass(BpmOALoanDO.class);
        verify(f.loanMapper).updateById(update.capture());
        assertEquals(BigDecimal.ZERO, update.getValue().getOutstandingAmount());
        assertEquals(2, update.getValue().getRepaymentStatus());
        assertNotNull(update.getValue().getRepaidTime());
    }

    private static BpmOALoanCreateReqVO request(String amount) {
        return new BpmOALoanCreateReqVO().setType("差旅借款").setAmount(new BigDecimal(amount))
                .setReason("客户现场差旅费用预支申请");
    }

    private static Fixture fixture() {
        BpmOALoanServiceImpl service = new BpmOALoanServiceImpl();
        BpmOALoanMapper loanMapper = mock(BpmOALoanMapper.class);
        BpmOALoanRepaymentMapper repaymentMapper = mock(BpmOALoanRepaymentMapper.class);
        BpmProcessInstanceApi processApi = mock(BpmProcessInstanceApi.class);
        AdminUserApi userApi = mock(AdminUserApi.class);
        PostApi postApi = mock(PostApi.class);
        BpmOALoanProperties properties = new BpmOALoanProperties();
        ReflectionTestUtils.setField(service, "loanMapper", loanMapper);
        ReflectionTestUtils.setField(service, "repaymentMapper", repaymentMapper);
        ReflectionTestUtils.setField(service, "processInstanceApi", processApi);
        ReflectionTestUtils.setField(service, "adminUserApi", userApi);
        ReflectionTestUtils.setField(service, "postApi", postApi);
        ReflectionTestUtils.setField(service, "tripService", mock(BpmOATripService.class));
        ReflectionTestUtils.setField(service, "properties", properties);
        return new Fixture(service, loanMapper, repaymentMapper, processApi, userApi, postApi);
    }

    private record Fixture(BpmOALoanServiceImpl service, BpmOALoanMapper loanMapper,
                           BpmOALoanRepaymentMapper repaymentMapper, BpmProcessInstanceApi processApi,
                           AdminUserApi userApi, PostApi postApi) {}
}

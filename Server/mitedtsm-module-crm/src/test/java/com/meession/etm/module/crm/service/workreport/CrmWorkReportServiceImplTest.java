package com.meession.etm.module.crm.service.workreport;

import com.meession.etm.module.crm.controller.admin.workreport.vo.CrmWorkReportSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.workreport.CrmWorkReportDO;
import com.meession.etm.module.crm.dal.mysql.workreport.CrmWorkReportMapper;
import com.meession.etm.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmWorkReportServiceImplTest {
    @Mock private CrmWorkReportMapper mapper;
    @Mock private AdminUserApi adminUserApi;
    @InjectMocks private CrmWorkReportServiceImpl service;

    @Test void createsWeeklyReportWithNormalizedPeriodAndRecipients() {
        doAnswer(invocation -> { ((CrmWorkReportDO) invocation.getArgument(0)).setId(9L); return 1; })
                .when(mapper).insert(any(CrmWorkReportDO.class));
        CrmWorkReportSaveReqVO req = request().setReportType(2).setReportDate(LocalDate.of(2026, 7, 17))
                .setReceiverUserIds(List.of(7L, 8L, 8L));
        assertEquals(9L, service.create(7L, req));
        verify(adminUserApi).validateUserList(List.of(8L));
        verify(mapper).insert(ArgumentMatchers.<CrmWorkReportDO>argThat(row ->
                row.getPeriodStart().equals(LocalDate.of(2026, 7, 13))
                        && row.getPeriodEnd().equals(LocalDate.of(2026, 7, 19))
                        && row.getReceiverUserIds().equals(List.of(8L)) && row.getStatus() == 0));
    }

    @Test void rejectsDuplicatePeriodAndMissingExternalRecipient() {
        when(mapper.selectByAuthorAndPeriod(eq(7L), eq(1), any())).thenReturn(new CrmWorkReportDO().setId(1L));
        assertServiceException(() -> service.create(7L, request()), WORK_REPORT_PERIOD_DUPLICATE);
        reset(mapper);
        CrmWorkReportSaveReqVO selfOnly = request().setReceiverUserIds(List.of(7L));
        assertServiceException(() -> service.create(7L, selfOnly), WORK_REPORT_RECEIVER_REQUIRED);
    }

    @Test void submittedReportCannotBeChangedOrDeleted() {
        when(mapper.selectById(9L)).thenReturn(new CrmWorkReportDO().setId(9L).setAuthorUserId(7L).setStatus(1));
        assertServiceException(() -> service.update(7L, request().setId(9L)), WORK_REPORT_SUBMITTED_IMMUTABLE);
        assertServiceException(() -> service.delete(7L, 9L), WORK_REPORT_SUBMITTED_IMMUTABLE);
    }

    @Test void authorAndRecipientCanReadButOtherUserCannot() {
        CrmWorkReportDO row = new CrmWorkReportDO().setId(9L).setAuthorUserId(7L).setReceiverUserIds(List.of(8L));
        when(mapper.selectById(9L)).thenReturn(row);
        assertSame(row, service.get(7L, 9L));
        assertSame(row, service.get(8L, 9L));
        assertServiceException(() -> service.get(10L, 9L), WORK_REPORT_NOT_EXISTS);
    }

    private CrmWorkReportSaveReqVO request() {
        return new CrmWorkReportSaveReqVO().setReportType(1).setReportDate(LocalDate.of(2026, 7, 17))
                .setTitle("7 月 17 日工作日报").setCompletedContent("完成客户需求评审和交付风险核对")
                .setNextPlan("推进剩余接口联调和运行验收").setReceiverUserIds(List.of(8L));
    }
}

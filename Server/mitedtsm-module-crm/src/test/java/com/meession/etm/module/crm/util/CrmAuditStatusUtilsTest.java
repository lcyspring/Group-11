package com.meession.etm.module.crm.util;

import com.meession.etm.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrmAuditStatusUtilsTest {

    @Test
    void convertsApproveResult() {
        assertEquals(CrmAuditStatusEnum.APPROVE.getStatus(), CrmAuditStatusUtils.convertBpmResultToAuditStatus(
                BpmProcessInstanceStatusEnum.APPROVE.getStatus()));
    }

    @Test
    void convertsRejectResult() {
        assertEquals(CrmAuditStatusEnum.REJECT.getStatus(), CrmAuditStatusUtils.convertBpmResultToAuditStatus(
                BpmProcessInstanceStatusEnum.REJECT.getStatus()));
    }

    @Test
    void convertsCancelResultToCrmValue() {
        assertEquals(CrmAuditStatusEnum.CANCEL.getStatus(), CrmAuditStatusUtils.convertBpmResultToAuditStatus(
                BpmProcessInstanceStatusEnum.CANCEL.getStatus()));
    }

    @Test
    void rejectsNonTerminalResult() {
        assertThrows(IllegalArgumentException.class, () -> CrmAuditStatusUtils.convertBpmResultToAuditStatus(
                BpmProcessInstanceStatusEnum.RUNNING.getStatus()));
    }

}

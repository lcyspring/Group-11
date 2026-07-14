package com.meession.etm.module.crm.enums;

import org.junit.jupiter.api.Test;

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.RECEIVABLE_CREATE_FAIL_PRICE_EXCEEDS_LIMIT;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.RECEIVABLE_PLAN_UPDATE_FAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmReceivableErrorCodeTest {

    @Test
    void receivableMessagesUseReceivableTerminology() {
        assertEquals("创建回款失败，原因：回款金额超出合同金额，目前剩余可回款：{} 元",
                RECEIVABLE_CREATE_FAIL_PRICE_EXCEEDS_LIMIT.getMsg());
        assertEquals("更新回款计划失败，原因：已经有关联回款", RECEIVABLE_PLAN_UPDATE_FAIL.getMsg());
    }

}

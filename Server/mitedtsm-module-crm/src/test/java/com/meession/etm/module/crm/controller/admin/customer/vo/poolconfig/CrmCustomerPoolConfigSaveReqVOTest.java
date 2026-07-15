package com.meession.etm.module.crm.controller.admin.customer.vo.poolconfig;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmCustomerPoolConfigSaveReqVOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsCompletePrototypePolicy() {
        assertEquals(0, validator.validate(validPolicy()).size());
    }

    @Test
    void rejectsInvalidExpiryLevelAndMinimumBatchBoundaries() {
        CrmCustomerPoolConfigSaveReqVO policy = validPolicy()
                .setContactExpireDays(0)
                .setHighValueLevelThreshold(6)
                .setAutoPoolBatchSize(0);

        assertEquals(3, validator.validate(policy).size());
    }

    @Test
    void requiresEnabledExpiryAndNotificationValues() {
        CrmCustomerPoolConfigSaveReqVO policy = validPolicy()
                .setContactExpireDays(null).setDealExpireDays(null).setNotifyDays(null);

        assertTrue(validator.validate(policy).stream().anyMatch(
                violation -> violation.getMessage().equals("未跟进放入公海天数不能为空")));
        assertTrue(validator.validate(policy).stream().anyMatch(
                violation -> violation.getMessage().equals("未成交放入公海天数不能为空")));
        assertTrue(validator.validate(policy).stream().anyMatch(
                violation -> violation.getMessage().equals("提前提醒天数不能为空")));
    }

    private static CrmCustomerPoolConfigSaveReqVO validPolicy() {
        return new CrmCustomerPoolConfigSaveReqVO()
                .setEnabled(true).setContactExpireDays(30).setDealExpireDays(30)
                .setNotifyEnabled(true).setNotifyDays(2)
                .setDailyClaimLimit(10).setRepeatClaimCooldownDays(30)
                .setHighValueLevelThreshold(4).setHighValueExpireMultiplier(2)
                .setProtectActiveBusiness(true).setProtectActiveContract(true)
                .setAutoPoolBatchSize(500);
    }
}

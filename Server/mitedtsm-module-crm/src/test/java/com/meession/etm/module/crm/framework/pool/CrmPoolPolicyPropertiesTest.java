package com.meession.etm.module.crm.framework.pool;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmPoolPolicyPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsDefaultBatchAtYamlSafetyLimit() {
        CrmPoolPolicyProperties.Customer customer = customerPolicy(5000, 5000);

        assertEquals(0, validator.validate(customer).size());
    }

    @Test
    void rejectsDefaultBatchAboveYamlSafetyLimit() {
        CrmPoolPolicyProperties.Customer customer = customerPolicy(5001, 5000);

        assertTrue(validator.validate(customer).stream().anyMatch(violation ->
                violation.getMessage().contains("must not exceed its YAML safety limit")));
    }

    @Test
    void validatesGarbageBatchAgainstYamlSafetyLimit() {
        CrmPoolPolicyProperties.Garbage garbage = new CrmPoolPolicyProperties.Garbage();
        garbage.setExpireDays(180);
        garbage.setMinimumPoolCycles(3);
        garbage.setBatchSize(5001);
        garbage.setMaxBatchSize(5000);
        garbage.setMaxBatches(20);

        assertTrue(validator.validate(garbage).stream().anyMatch(violation ->
                violation.getMessage().contains("garbage batch size must not exceed")));
    }

    private static CrmPoolPolicyProperties.Customer customerPolicy(int batchSize, int maxBatchSize) {
        CrmPoolPolicyProperties.Customer customer = new CrmPoolPolicyProperties.Customer();
        customer.setContactExpireDays(30);
        customer.setDealExpireDays(30);
        customer.setHighValueLevelThreshold(4);
        customer.setHighValueExpireMultiplier(2);
        customer.setProtectedContractAuditStatuses(java.util.List.of(0, 10, 20));
        customer.setDailyClaimLimit(10);
        customer.setRepeatClaimCooldownDays(30);
        customer.setNotifyDays(2);
        customer.setAutoPoolBatchSize(batchSize);
        customer.setAutoPoolMaxBatchSize(maxBatchSize);
        customer.setAutoPoolMaxBatches(20);
        return customer;
    }
}

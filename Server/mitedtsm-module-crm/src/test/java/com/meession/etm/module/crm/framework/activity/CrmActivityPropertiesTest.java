package com.meession.etm.module.crm.framework.activity;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmActivityPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidYamlPolicy() {
        assertEquals(0, validator.validate(policy(500, 5000, "Asia/Shanghai")).size());
    }

    @Test
    void rejectsBatchAboveYamlSafetyLimit() {
        assertTrue(validator.validate(policy(5001, 5000, "Asia/Shanghai")).stream().anyMatch(violation ->
                violation.getMessage().contains("must not exceed its YAML safety limit")));
    }

    @Test
    void rejectsInvalidSchedulerZone() {
        assertTrue(validator.validate(policy(500, 5000, "Mars/Olympus")).stream().anyMatch(violation ->
                violation.getMessage().contains("valid ZoneId")));
    }

    private static CrmActivityProperties.TaskOverdue policy(int batch, int max, String zone) {
        CrmActivityProperties.TaskOverdue policy = new CrmActivityProperties.TaskOverdue();
        policy.setCron("0 0 * * * ?");
        policy.setZone(zone);
        policy.setLockKey("crm:activity:task-overdue");
        policy.setLockLeaseSeconds(1800);
        policy.setBatchSize(batch);
        policy.setMaxBatchSize(max);
        policy.setMaxBatches(20);
        return policy;
    }
}

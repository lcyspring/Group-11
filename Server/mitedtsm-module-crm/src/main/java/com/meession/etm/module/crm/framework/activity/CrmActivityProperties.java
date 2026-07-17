package com.meession.etm.module.crm.framework.activity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

@Component
@Data
@Validated
@ConfigurationProperties(prefix = "mitedtsm.crm.activity")
public class CrmActivityProperties {
    @NotBlank
    private String version;
    @NotBlank
    private String protectedCallRecordingDirectory;
    @Min(1)
    private int maxCallDurationSeconds;
    @Valid
    @NotNull
    private TaskOverdue taskOverdue = new TaskOverdue();
    @Valid
    @NotNull
    private ReceivableOverdue receivableOverdue = new ReceivableOverdue();

    @Data
    public static class TaskOverdue {
        private boolean enabled;
        @NotBlank
        private String cron;
        @NotBlank
        private String zone;
        @NotBlank
        private String lockKey;
        @Min(60)
        private int lockLeaseSeconds;
        @Min(1)
        private int batchSize;
        @Min(1)
        private int maxBatchSize;
        @Min(1)
        @Max(100)
        private int maxBatches;

        @AssertTrue(message = "CRM task overdue batch size must not exceed its YAML safety limit")
        public boolean isBatchSizeWithinSafetyLimit() {
            return batchSize <= maxBatchSize;
        }

        @AssertTrue(message = "CRM task overdue scheduler zone must be a valid ZoneId")
        public boolean isZoneValid() {
            try {
                ZoneId.of(zone);
                return true;
            } catch (RuntimeException ex) {
                return false;
            }
        }
    }

    @Data
    public static class ReceivableOverdue {
        private boolean enabled;
        @NotBlank
        private String cron;
        @NotBlank
        private String zone;
        @NotBlank
        private String lockKey;
        @Min(60)
        private int lockLeaseSeconds;
        @Min(1)
        private int batchSize;
        @Min(1)
        private int maxBatchSize;
        @Min(1)
        @Max(100)
        private int maxBatches;
        @Min(1)
        private int maxRetries;

        @AssertTrue(message = "CRM receivable overdue batch size must not exceed its YAML safety limit")
        public boolean isBatchSizeWithinSafetyLimit() {
            return batchSize <= maxBatchSize;
        }

        @AssertTrue(message = "CRM receivable overdue scheduler zone must be a valid ZoneId")
        public boolean isZoneValid() {
            try {
                ZoneId.of(zone);
                return true;
            } catch (RuntimeException ex) {
                return false;
            }
        }
    }
}

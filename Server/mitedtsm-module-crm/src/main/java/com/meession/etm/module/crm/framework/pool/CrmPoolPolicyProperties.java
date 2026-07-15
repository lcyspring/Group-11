package com.meession.etm.module.crm.framework.pool;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Explicit CRM public-pool, garbage-pool and scheduler policy. */
@Component
@Data
@Validated
@ConfigurationProperties(prefix = "mitedtsm.crm.pool-policy")
public class CrmPoolPolicyProperties {

    @NotBlank
    private String version;

    @Valid
    @NotNull
    private Customer customer = new Customer();

    @Valid
    @NotNull
    private Garbage garbage = new Garbage();

    @Valid
    @NotNull
    private Clue clue = new Clue();

    @Valid
    @NotNull
    private Scheduler scheduler = new Scheduler();

    @Data
    public static class Customer {
        private boolean enabled;
        @Min(1)
        @Max(3650)
        private int contactExpireDays;
        @Min(1)
        @Max(3650)
        private int dealExpireDays;
        @Min(1)
        @Max(5)
        private int highValueLevelThreshold;
        @Min(1)
        @Max(10)
        private int highValueExpireMultiplier;
        private boolean protectActiveBusiness;
        private boolean protectActiveContract;
        @NotEmpty
        private List<Integer> protectedContractAuditStatuses = new ArrayList<>();
        @Min(1)
        @Max(1000)
        private int dailyClaimLimit;
        @Min(0)
        @Max(3650)
        private int repeatClaimCooldownDays;
        private boolean notifyEnabled;
        @Min(1)
        @Max(3650)
        private int notifyDays;
        @Min(1)
        private int autoPoolBatchSize;
        @Min(1)
        private int autoPoolMaxBatchSize;
        @Min(1)
        @Max(100)
        private int autoPoolMaxBatches;

        @AssertTrue(message = "CRM customer pool batch size must not exceed its YAML safety limit")
        public boolean isAutoPoolBatchSizeWithinSafetyLimit() {
            return autoPoolBatchSize <= autoPoolMaxBatchSize;
        }
    }

    @Data
    public static class Garbage {
        private boolean autoEnabled;
        @Min(1)
        private int expireDays;
        @Min(1)
        private int minimumPoolCycles;
        @Min(1)
        @Max(5000)
        private int batchSize;
    }

    @Data
    public static class Clue {
        @Min(1)
        private int contactExpireDays;
        @Min(1)
        private int dailyClaimLimit;
        @Min(0)
        private int repeatClaimCooldownDays;
        @Min(1)
        @Max(5000)
        private int autoPoolBatchSize;
    }

    @Data
    public static class Scheduler {
        private boolean enabled;
        @NotBlank
        private String cron;
        @NotBlank
        private String zone;
        @NotBlank
        private String lockKey;
        @Min(60)
        private int lockLeaseSeconds;

        @AssertTrue(message = "CRM pool scheduler zone must be a valid ZoneId")
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

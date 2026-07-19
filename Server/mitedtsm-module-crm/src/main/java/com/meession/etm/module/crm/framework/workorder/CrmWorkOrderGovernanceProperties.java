package com.meession.etm.module.crm.framework.workorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.LocalTime;
import java.time.ZoneId;

@Component
@Data
@Validated
@ConfigurationProperties(prefix = "mitedtsm.crm.work-order-governance")
public class CrmWorkOrderGovernanceProperties {

    @Valid
    @NotNull
    private Geofence geofence = new Geofence();
    @Valid
    @NotNull
    private Sla sla = new Sla();

    @Data
    public static class Geofence {
        private boolean enabled;
        private boolean requireBeforeComplete;
        @Min(10)
        private int defaultRadiusMeters = 300;
        @Min(1)
        private int maxAccuracyMeters = 100;
    }

    @Data
    public static class Sla {
        private boolean enabled;
        private boolean skipWeekends = true;
        @NotBlank
        private String zone = "Asia/Shanghai";
        @NotNull
        private LocalTime workdayStart = LocalTime.of(9, 0);
        @NotNull
        private LocalTime workdayEnd = LocalTime.of(18, 0);
        @NotBlank
        private String defaultPolicyCode = "MEDIUM";
        @NotBlank
        private String cron = "0 */5 * * * ?";
        @NotBlank
        private String lockKey = "crm:work-order:sla";
        @Min(60)
        private int lockLeaseSeconds = 300;

        @AssertTrue(message = "CRM work-order SLA zone must be valid")
        public boolean isZoneValid() {
            try {
                ZoneId.of(zone);
                return true;
            } catch (RuntimeException ex) {
                return false;
            }
        }

        @AssertTrue(message = "CRM work-order SLA workday end must be after start")
        public boolean isWorkdayRangeValid() {
            return workdayStart != null && workdayEnd != null && workdayEnd.isAfter(workdayStart);
        }
    }
}

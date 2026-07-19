package com.meession.etm.module.crm.framework.workorder;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Data
@Validated
@ConfigurationProperties(prefix = "mitedtsm.crm.work-order-dispatch")
public class CrmWorkOrderDispatchProperties {

    private boolean enabled;
    private boolean autoAssignOnCreate;
    @NotNull
    private FallbackMode fallbackMode;
    @Min(1)
    @Max(100)
    private int maxCcUsers;
    @Min(20)
    @Max(5000)
    private int descriptionMinLength;
    @Min(20)
    @Max(5000)
    private int solutionMinLength;

    public enum FallbackMode {
        UNASSIGNED_POOL,
        REQUIRE_HANDLER
    }
}

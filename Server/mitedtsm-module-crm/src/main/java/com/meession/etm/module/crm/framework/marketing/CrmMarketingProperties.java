package com.meession.etm.module.crm.framework.marketing;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Data
@Validated
@ConfigurationProperties(prefix = "mitedtsm.crm.marketing")
public class CrmMarketingProperties {
    @NotBlank
    private String providerMode = "record-only";
    @Min(1)
    private int batchSize = 50;
    @Min(1)
    private int maxBatchSize = 500;
    @Min(1)
    private int perRecipientDailyLimit = 1;
    @Min(1)
    private int monthlyRecipientLimit = 10000;
    @NotBlank
    private String careCron = "0 0 9 * * ?";
    @NotBlank
    private String careZone = "Asia/Shanghai";
    @NotBlank
    private String lockKey = "crm:marketing:care";
    @Min(60)
    private int lockLeaseSeconds = 300;

    @AssertTrue(message = "CRM marketing batch size must not exceed max-batch-size")
    public boolean isBatchWithinLimit() {
        return batchSize <= maxBatchSize;
    }

    @AssertTrue(message = "CRM marketing provider-mode must be record-only or system")
    public boolean isProviderModeValid() {
        return "record-only".equals(providerMode) || "system".equals(providerMode);
    }
}

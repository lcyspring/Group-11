package com.meession.etm.module.crm.framework.exporttask;

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
@ConfigurationProperties(prefix = "mitedtsm.crm.export-task")
public class CrmExportTaskProperties {
    private boolean enabled = true;
    @Min(1) private int batchSize = 10;
    @Min(1) private int maxBatchSize = 100;
    @Min(1) private int maxPendingPerUser = 3;
    @Min(1) private int maxRows = 5000;
    @Min(1) private int retentionHours = 24;
    @Min(30) private int tokenTtlSeconds = 300;
    @NotBlank private String cron = "0/5 * * * * ?";
    @NotBlank private String zone = "Asia/Shanghai";
    @NotBlank private String lockKey = "crm:export-task:scheduler";
    @Min(30) private int lockLeaseSeconds = 300;

    @AssertTrue(message = "CRM export task batch-size must not exceed max-batch-size")
    public boolean isBatchWithinLimit() {
        return batchSize <= maxBatchSize;
    }
}

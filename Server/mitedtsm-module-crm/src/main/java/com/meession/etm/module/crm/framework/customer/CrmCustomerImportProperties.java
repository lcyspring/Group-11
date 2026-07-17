package com.meession.etm.module.crm.framework.customer;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Data
@Validated
@ConfigurationProperties(prefix = "mitedtsm.crm.customer-import")
public class CrmCustomerImportProperties {

    @Min(1)
    @Max(10000)
    private int maxRows = 2000;

    @Min(5)
    @Max(1440)
    private int previewTtlMinutes = 60;
}

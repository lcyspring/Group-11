package com.meession.etm.module.crm.framework.refund;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Data
@Validated
@ConfigurationProperties(prefix = "mitedtsm.crm.receivable-refund")
public class CrmReceivableRefundProperties {

    @NotBlank
    private String processDefinitionKey;

    @NotBlank
    private String numberPrefix;
}

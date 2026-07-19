package com.meession.etm.module.crm.framework.contract;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Data
@Validated
@ConfigurationProperties(prefix = "mitedtsm.crm.contract-amendment")
public class CrmContractAmendmentProperties {

    @NotBlank
    private String processDefinitionKey;

    @NotBlank
    private String numberPrefix;
}

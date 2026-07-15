package com.meession.etm.module.crm.framework.reimbursement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Data
@Validated
@ConfigurationProperties(prefix = "mitedtsm.crm.reimbursement")
public class CrmReimbursementProperties {

    @NotBlank
    private String processDefinitionKey;

    @NotBlank
    private String numberPrefix;

    @NotBlank
    @Pattern(regexp = "[A-Z]{3}")
    private String defaultCurrency;

    @NotBlank
    private String protectedFileDirectory;
}

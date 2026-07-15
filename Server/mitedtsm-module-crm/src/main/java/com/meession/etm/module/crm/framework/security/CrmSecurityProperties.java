package com.meession.etm.module.crm.framework.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Data
@Validated
@ConfigurationProperties(prefix = "mitedtsm.crm.security")
public class CrmSecurityProperties {

    /** Managed-file directory that is never served by the public infra route. */
    @NotBlank
    private String protectedFileDirectory;
}

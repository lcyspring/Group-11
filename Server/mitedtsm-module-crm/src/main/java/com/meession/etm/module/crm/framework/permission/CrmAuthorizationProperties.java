package com.meession.etm.module.crm.framework.permission;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * CRM authorization configuration.
 */
@Component
@Data
@Validated
@ConfigurationProperties(prefix = "mitedtsm.crm.authorization")
public class CrmAuthorizationProperties {

    /** Role codes that bypass CRM object and organization data scopes. */
    @NotEmpty
    private List<String> adminRoleCodes = new ArrayList<>();

}

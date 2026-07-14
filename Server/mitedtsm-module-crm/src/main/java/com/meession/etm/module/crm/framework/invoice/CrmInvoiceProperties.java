package com.meession.etm.module.crm.framework.invoice;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;

@Component
@Validated
@ConfigurationProperties(prefix = "mitedtsm.crm.invoice")
public class CrmInvoiceProperties {

    @NotBlank(message = "mitedtsm.crm.invoice.provider 不能为空")
    private String provider;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}

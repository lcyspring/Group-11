package com.meession.etm.framework.security.config;

import com.meession.etm.framework.security.core.filter.IpsFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IpsFilterConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IpsFilter ipsFilter() {
        return new IpsFilter();
    }
}

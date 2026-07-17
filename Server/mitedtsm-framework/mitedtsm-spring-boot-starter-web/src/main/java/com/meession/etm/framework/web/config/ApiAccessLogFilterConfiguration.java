package com.meession.etm.framework.web.config;

import com.meession.etm.framework.web.core.filter.ApiAccessLogFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiAccessLogFilterConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ApiAccessLogFilter apiAccessLogFilter() {
        return new ApiAccessLogFilter();
    }
}

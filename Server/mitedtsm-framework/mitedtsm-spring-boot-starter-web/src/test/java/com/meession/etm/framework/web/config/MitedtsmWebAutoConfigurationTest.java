package com.meession.etm.framework.web.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MitedtsmWebAutoConfigurationTest {

    @Test
    void shouldBuildExplicitCorsPolicyWithoutWildcardFallback() {
        WebProperties.Cors properties = new WebProperties.Cors();
        properties.setAllowedOriginPatterns(List.of("http://127.0.0.1:8081", "http://localhost:8081"));
        properties.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        properties.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        properties.setAllowCredentials(true);
        properties.setMaxAge(Duration.ofMinutes(30));

        CorsConfiguration configuration = MitedtsmWebAutoConfiguration.buildCorsConfiguration(properties);

        assertEquals(properties.getAllowedOriginPatterns(), configuration.getAllowedOriginPatterns());
        assertEquals(properties.getAllowedHeaders(), configuration.getAllowedHeaders());
        assertEquals(properties.getAllowedMethods(), configuration.getAllowedMethods());
        assertEquals(properties.getAllowCredentials(), configuration.getAllowCredentials());
        assertEquals(properties.getMaxAge().getSeconds(), configuration.getMaxAge());
    }

    @Test
    void shouldDenyCrossOriginByDefault() {
        CorsConfiguration configuration = MitedtsmWebAutoConfiguration
                .buildCorsConfiguration(new WebProperties.Cors());

        assertEquals(List.of(), configuration.getAllowedOriginPatterns());
    }
}

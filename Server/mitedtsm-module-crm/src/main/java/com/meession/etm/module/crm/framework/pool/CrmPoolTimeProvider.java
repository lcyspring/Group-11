package com.meession.etm.module.crm.framework.pool;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Single, configurable clock boundary for pool rules and tests. */
@Component
@RequiredArgsConstructor
public class CrmPoolTimeProvider {

    private final CrmPoolPolicyProperties properties;

    public LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of(properties.getScheduler().getZone()));
    }
}

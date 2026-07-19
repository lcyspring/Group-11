package com.meession.etm.module.bpm.framework.oa;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
@Data
@Validated
@ConfigurationProperties(prefix = "mitedtsm.bpm.oa.leave")
public class BpmOALeaveProperties {

    @NotEmpty
    private Set<Integer> balanceRequiredTypes = Set.of(4, 5);

    @NotEmpty
    private Map<Integer, Long> defaultAnnualDays = new LinkedHashMap<>(Map.of(4, 5L, 5, 0L));
}

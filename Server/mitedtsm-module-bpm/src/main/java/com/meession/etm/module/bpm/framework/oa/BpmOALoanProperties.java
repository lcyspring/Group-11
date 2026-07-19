package com.meession.etm.module.bpm.framework.oa;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "mitedtsm.bpm.oa.loan")
@Data
public class BpmOALoanProperties {
    private BigDecimal employeeLimit = new BigDecimal("5000");
    private BigDecimal managerLimit = new BigDecimal("20000");
    private BigDecimal directorLimit = new BigDecimal("50000");
    private Set<String> managerPostCodes = new LinkedHashSet<>(Set.of("manager"));
    private Set<String> directorPostCodes = new LinkedHashSet<>(Set.of("director"));
}

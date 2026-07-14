package com.meession.etm.module.crm.framework.statistics;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * CRM 统计指标口径与血缘配置。
 *
 * <p>配置只描述已经落地的查询口径，不承载 SQL，也不允许通过接口修改。</p>
 */
@Component
@Data
@Validated
@ConfigurationProperties(prefix = "mitedtsm.crm.statistics-metadata")
public class CrmStatisticsMetadataProperties {

    @NotBlank
    private String refreshMode;

    @NotBlank
    private String permissionMode;

    @NotBlank
    private String historyRecalculation;

    @NotBlank
    private String reconciliation;

    @Valid
    @NotEmpty
    private List<Metric> metrics = new ArrayList<>();

    @Data
    public static class Metric {

        @NotBlank
        private String scope;

        @NotBlank
        private String code;

        @NotBlank
        private String name;

        @NotEmpty
        private List<@NotBlank String> sourceTables = new ArrayList<>();

        @NotEmpty
        private List<@NotBlank String> sourceFields = new ArrayList<>();

        @NotBlank
        private String businessTime;

        @NotBlank
        private String formula;

        @NotEmpty
        private List<@NotBlank String> filters = new ArrayList<>();

        @NotBlank
        private String permission;
    }
}

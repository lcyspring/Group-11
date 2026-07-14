package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.metadata.CrmStatisticsMetadataCatalogRespVO;
import com.meession.etm.module.crm.framework.statistics.CrmStatisticsMetadataProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmStatisticsMetadataServiceImplTest {

    @Test
    void bindExplicitConfiguration() {
        CrmStatisticsMetadataProperties properties = bindProperties();

        assertEquals("REALTIME_QUERY", properties.getRefreshMode());
        assertEquals("CRM_TENANT_AND_DATA_SCOPE", properties.getPermissionMode());
        assertEquals(2, properties.getMetrics().size());
        assertEquals(List.of("crm_business"), properties.getMetrics().get(0).getSourceTables());
        assertEquals("crm_business.create_time", properties.getMetrics().get(0).getBusinessTime());
    }

    @Test
    void getCatalogOnlyReturnsRequestedScope() {
        CrmStatisticsMetadataServiceImpl service = new CrmStatisticsMetadataServiceImpl(bindProperties());
        service.validateMetricIdentity();

        CrmStatisticsMetadataCatalogRespVO result = service.getCatalog(" FUNNEL ");

        assertEquals("funnel", result.getScope());
        assertEquals("REALTIME_QUERY", result.getRefreshMode());
        assertEquals("CRM_TENANT_AND_DATA_SCOPE", result.getPermissionMode());
        assertNotNull(result.getGeneratedAt());
        assertEquals(1, result.getMetrics().size());
        assertEquals("funnel.business.new-count", result.getMetrics().get(0).getCode());
        assertTrue(result.getMetrics().stream().noneMatch(metric -> metric.getCode().startsWith("customer.")));
    }

    @Test
    void rejectDuplicateCodeInsideScope() {
        CrmStatisticsMetadataProperties properties = bindProperties();
        CrmStatisticsMetadataProperties.Metric duplicate = metric("funnel", "funnel.business.new-count");
        properties.getMetrics().add(duplicate);
        CrmStatisticsMetadataServiceImpl service = new CrmStatisticsMetadataServiceImpl(properties);

        IllegalStateException exception = assertThrows(IllegalStateException.class, service::validateMetricIdentity);
        assertTrue(exception.getMessage().contains("funnel:funnel.business.new-count"));
    }

    private static CrmStatisticsMetadataProperties bindProperties() {
        Map<String, Object> values = new LinkedHashMap<>();
        String prefix = "mitedtsm.crm.statistics-metadata.";
        values.put(prefix + "refresh-mode", "REALTIME_QUERY");
        values.put(prefix + "permission-mode", "CRM_TENANT_AND_DATA_SCOPE");
        values.put(prefix + "history-recalculation", "QUERY_CURRENT_LEDGER_STATE_ONLY");
        values.put(prefix + "reconciliation", "DRILLDOWN_OR_SOURCE_LEDGER_EXPORT");
        putMetric(values, prefix, 0, metric("funnel", "funnel.business.new-count"));
        putMetric(values, prefix, 1, metric("customer", "customer.new-count"));
        return new Binder(new MapConfigurationPropertySource(values))
                .bind(prefix.substring(0, prefix.length() - 1), Bindable.of(CrmStatisticsMetadataProperties.class))
                .orElseThrow(() -> new AssertionError("统计指标配置未绑定"));
    }

    private static CrmStatisticsMetadataProperties.Metric metric(String scope, String code) {
        CrmStatisticsMetadataProperties.Metric metric = new CrmStatisticsMetadataProperties.Metric();
        metric.setScope(scope);
        metric.setCode(code);
        metric.setName(code);
        metric.setSourceTables(List.of(scope.equals("funnel") ? "crm_business" : "crm_customer"));
        metric.setSourceFields(List.of("id", "create_time"));
        metric.setBusinessTime(scope.equals("funnel") ? "crm_business.create_time" : "crm_customer.create_time");
        metric.setFormula("COUNT(id)");
        metric.setFilters(List.of("deleted = 0"));
        metric.setPermission("tenant + data scope");
        return metric;
    }

    private static void putMetric(Map<String, Object> values, String prefix, int index,
                                  CrmStatisticsMetadataProperties.Metric metric) {
        String item = prefix + "metrics[" + index + "].";
        values.put(item + "scope", metric.getScope());
        values.put(item + "code", metric.getCode());
        values.put(item + "name", metric.getName());
        values.put(item + "source-tables[0]", metric.getSourceTables().get(0));
        values.put(item + "source-fields[0]", metric.getSourceFields().get(0));
        values.put(item + "source-fields[1]", metric.getSourceFields().get(1));
        values.put(item + "business-time", metric.getBusinessTime());
        values.put(item + "formula", metric.getFormula());
        values.put(item + "filters[0]", metric.getFilters().get(0));
        values.put(item + "permission", metric.getPermission());
    }
}

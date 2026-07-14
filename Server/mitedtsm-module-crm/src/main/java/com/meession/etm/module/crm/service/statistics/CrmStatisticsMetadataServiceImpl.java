package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.metadata.CrmStatisticsMetadataCatalogRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.metadata.CrmStatisticsMetricMetadataRespVO;
import com.meession.etm.module.crm.framework.statistics.CrmStatisticsMetadataProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CrmStatisticsMetadataServiceImpl implements CrmStatisticsMetadataService {

    private final CrmStatisticsMetadataProperties properties;

    @PostConstruct
    void validateMetricIdentity() {
        Set<String> identities = new HashSet<>();
        for (CrmStatisticsMetadataProperties.Metric metric : properties.getMetrics()) {
            String identity = normalize(metric.getScope()) + ":" + metric.getCode();
            if (!identities.add(identity)) {
                throw new IllegalStateException("CRM 统计指标编码重复: " + identity);
            }
        }
    }

    @Override
    public CrmStatisticsMetadataCatalogRespVO getCatalog(String scope) {
        String normalizedScope = normalize(scope);
        List<CrmStatisticsMetricMetadataRespVO> metrics = properties.getMetrics().stream()
                .filter(metric -> normalize(metric.getScope()).equals(normalizedScope))
                .map(CrmStatisticsMetadataServiceImpl::toRespVO)
                .toList();
        return new CrmStatisticsMetadataCatalogRespVO()
                .setGeneratedAt(LocalDateTime.now())
                .setScope(normalizedScope)
                .setRefreshMode(properties.getRefreshMode())
                .setPermissionMode(properties.getPermissionMode())
                .setHistoryRecalculation(properties.getHistoryRecalculation())
                .setReconciliation(properties.getReconciliation())
                .setMetrics(metrics);
    }

    private static CrmStatisticsMetricMetadataRespVO toRespVO(CrmStatisticsMetadataProperties.Metric metric) {
        return new CrmStatisticsMetricMetadataRespVO()
                .setCode(metric.getCode())
                .setName(metric.getName())
                .setSourceTables(List.copyOf(metric.getSourceTables()))
                .setSourceFields(List.copyOf(metric.getSourceFields()))
                .setBusinessTime(metric.getBusinessTime())
                .setFormula(metric.getFormula())
                .setFilters(List.copyOf(metric.getFilters()))
                .setPermission(metric.getPermission());
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

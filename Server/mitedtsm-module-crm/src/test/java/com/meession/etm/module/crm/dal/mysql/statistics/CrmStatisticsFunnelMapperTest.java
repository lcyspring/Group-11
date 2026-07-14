package com.meession.etm.module.crm.dal.mysql.statistics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmStatisticsFunnelMapperTest {

    private static final String MAPPER_RESOURCE = "/mapper/statistics/CrmStatisticsFunnelMapper.xml";

    @Test
    void amountSummariesReturnZeroForNullAmounts() throws IOException {
        String mapperXml = loadMapperXml();

        List.of(
                "selectBusinessSummaryListGroupByEndStatus",
                "selectBusinessSummaryGroupByDate"
        ).forEach(id -> assertTrue(selectBody(mapperXml, id).contains("COALESCE(SUM(total_price), 0)"),
                () -> id + " 必须将空商机金额聚合为 0"));
    }

    @Test
    void salesForecastUsesExpectedDealTimeStageProbabilityAndActiveBusinesses() throws IOException {
        String body = selectBody(loadMapperXml(), "selectBusinessForecastGroupByDate");

        assertTrue(body.contains("business.deleted = 0"));
        assertTrue(body.contains("business.end_status IS NULL"));
        assertTrue(body.contains("business.deal_time BETWEEN"));
        assertTrue(body.contains("status.percent"));
        assertTrue(body.contains("business.total_price"));
    }

    private static String loadMapperXml() throws IOException {
        try (InputStream input = CrmStatisticsFunnelMapperTest.class.getResourceAsStream(MAPPER_RESOURCE)) {
            assertNotNull(input, "漏斗统计 Mapper XML 应在测试 classpath 中");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String selectBody(String mapperXml, String id) {
        int start = mapperXml.indexOf("<select id=\"" + id + "\"");
        assertTrue(start >= 0, () -> "缺少统计 SQL: " + id);
        int end = mapperXml.indexOf("</select>", start);
        assertTrue(end > start, () -> "统计 SQL 未闭合: " + id);
        return mapperXml.substring(start, end);
    }
}

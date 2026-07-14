package com.meession.etm.module.crm.dal.mysql.statistics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmStatisticsPortraitMapperTest {

    @Test
    void dealStatusSummaryEnforcesTenantScopedQueryInputs() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/mapper/statistics/CrmStatisticsPortraitMapper.xml")) {
            assertNotNull(input, "画像统计 Mapper XML 应在测试 classpath 中");
            String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            String body = selectBody(xml, "selectCustomerDealStatusList");
            assertTrue(body.contains("deleted = 0"));
            assertTrue(body.contains("owner_user_id IN"));
            assertTrue(body.contains("create_time BETWEEN"));
            assertTrue(body.contains("GROUP BY deal_status"));
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

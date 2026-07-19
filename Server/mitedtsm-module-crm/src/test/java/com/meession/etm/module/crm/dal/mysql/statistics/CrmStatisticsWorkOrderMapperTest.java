package com.meession.etm.module.crm.dal.mysql.statistics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CrmStatisticsWorkOrderMapperTest {
    private static final String RESOURCE = "/mapper/statistics/CrmStatisticsWorkOrderMapper.xml";

    @Test
    void everyQueryHasTenantDeletionPersonalScopeAndBusinessTime() throws IOException {
        String xml = load();
        String scope = sqlBody(xml, "scope");
        assertAll(
                () -> assertTrue(scope.contains("tenant_id = #{tenantId}")),
                () -> assertTrue(scope.contains("deleted = 0")),
                () -> assertTrue(scope.contains("creator = CAST(#{userId} AS CHAR)")),
                () -> assertTrue(scope.contains("handler_user_id = #{userId}")),
                () -> assertTrue(scope.contains("#{queryAll} = true"))
        );
        List.of("selectSummary", "selectByStatus", "selectByType", "selectByHandler").forEach(id ->
                assertTrue(selectBody(xml, id).contains("create_time BETWEEN"), id + " 必须按创建时间筛选"));
        String trend = selectBody(xml, "selectTrend");
        assertTrue(trend.contains("complete_time IS NOT NULL"));
        assertTrue(trend.contains("complete_time BETWEEN"));
        assertTrue(trend.contains("GROUP BY DATE_FORMAT(create_time"));
        assertTrue(trend.contains("GROUP BY DATE_FORMAT(complete_time"));
        assertFalse(trend.contains("%H:%i:%s"), "SELECT 与 GROUP BY 必须使用同一日桶表达式");
        assertFalse(trend.contains("crm_work_order_record"), "统计不能连接轨迹表造成重复计数");
    }

    private static String load() throws IOException {
        try (InputStream input = CrmStatisticsWorkOrderMapperTest.class.getResourceAsStream(RESOURCE)) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String sqlBody(String xml, String id) {
        int start = xml.indexOf("<sql id=\"" + id + "\"");
        int end = xml.indexOf("</sql>", start);
        assertTrue(start >= 0 && end > start);
        return xml.substring(start, end);
    }

    private static String selectBody(String xml, String id) {
        int start = xml.indexOf("<select id=\"" + id + "\"");
        int end = xml.indexOf("</select>", start);
        assertTrue(start >= 0 && end > start);
        return xml.substring(start, end);
    }
}

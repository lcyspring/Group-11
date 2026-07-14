package com.meession.etm.module.crm.dal.mysql.statistics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmStatisticsPerformanceMapperTest {

    private static final String MAPPER_RESOURCE = "/mapper/statistics/CrmStatisticsPerformanceMapper.xml";

    @Test
    void targetActualQueriesUseExpectedBusinessTimeOwnerAndDeletionRules() throws IOException {
        String mapperXml = loadMapperXml();

        String contract = selectBody(mapperXml, "selectContractPricePerformance");
        assertTrue(contract.contains("audit_status"));
        assertTrue(contract.contains("owner_user_id in"));
        assertTrue(contract.contains("order_date between"));

        String receivable = selectBody(mapperXml, "selectReceivablePricePerformance");
        assertTrue(receivable.contains("audit_status"));
        assertTrue(receivable.contains("owner_user_id in"));
        assertTrue(receivable.contains("return_time between"));

        List.of("selectFollowUpCountPerformance", "selectCustomerCountPerformance",
                "selectBusinessCountPerformance").forEach(id -> {
            String body = selectBody(mapperXml, id);
            assertTrue(body.contains("deleted = 0"), () -> id + " 必须排除逻辑删除记录");
            assertTrue(body.contains("create_time BETWEEN"), () -> id + " 必须使用创建时间");
        });
        String followUp = selectBody(mapperXml, "selectFollowUpCountPerformance");
        assertTrue(followUp.contains("biz_type"));
        assertTrue(followUp.contains("creator IN"));
    }

    private static String loadMapperXml() throws IOException {
        try (InputStream input = CrmStatisticsPerformanceMapperTest.class.getResourceAsStream(MAPPER_RESOURCE)) {
            assertNotNull(input, "业绩统计 Mapper XML 应在测试 classpath 中");
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

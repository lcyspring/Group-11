package com.meession.etm.module.crm.dal.mysql.statistics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmStatisticsCustomerMapperTest {

    private static final String MAPPER_RESOURCE = "/mapper/statistics/CrmStatisticsCustomerMapper.xml";

    @Test
    void followUpQueriesIgnoreLogicallyDeletedRecords() throws IOException {
        String mapperXml;
        try (InputStream input = CrmStatisticsCustomerMapperTest.class.getResourceAsStream(MAPPER_RESOURCE)) {
            assertNotNull(input, "统计 Mapper XML 应在测试 classpath 中");
            mapperXml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        List.of(
                "selectFollowUpRecordCountGroupByDate",
                "selectFollowUpCustomerCountGroupByDate",
                "selectFollowUpRecordCountGroupByUser",
                "selectFollowUpCustomerCountGroupByUser",
                "selectFollowUpRecordCountGroupByType"
        ).forEach(id -> assertTrue(selectBody(mapperXml, id).contains("deleted = 0"),
                () -> id + " 必须排除逻辑删除跟进记录"));
    }

    private static String selectBody(String mapperXml, String id) {
        int start = mapperXml.indexOf("<select id=\"" + id + "\"");
        assertTrue(start >= 0, () -> "缺少统计 SQL: " + id);
        int end = mapperXml.indexOf("</select>", start);
        assertTrue(end > start, () -> "统计 SQL 未闭合: " + id);
        return mapperXml.substring(start, end);
    }

}

package com.meession.etm.module.crm.dal.mysql.customer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmCustomer360MapperTest {

    @Test
    void summaryDeclaresStatusesSoftDeleteAndWorkOrderScope() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/mapper/customer/CrmCustomer360Mapper.xml")) {
            assertNotNull(input, "客户 360 Mapper XML 应在测试 classpath 中");
            String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(xml.contains("item.audit_status = 20"));
            assertTrue(xml.contains("FROM crm_receivable_refund item"));
            assertTrue(xml.contains("AS refundCount"));
            assertTrue(xml.contains("AS approvedRefundAmount"));
            assertTrue(xml.contains("item.status IN (10, 20, 30)"));
            assertTrue(xml.contains("item.direction * item.amount"));
            assertTrue(xml.contains("attachment.deleted = 0"));
            assertTrue(xml.contains("!queryAllWorkOrders"));
            assertTrue(xml.contains("item.handler_user_id = #{userId}"));
        }
    }
}

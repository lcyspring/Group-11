package com.meession.etm.module.crm.dal.mysql.customer;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** Central reference guard used before irreversible customer deletion. */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface CrmCustomerReferenceMapper {

    @Select("""
            SELECT reference_name
            FROM (
                SELECT 1 sort_no, '下级客户' reference_name FROM DUAL WHERE EXISTS (
                    SELECT 1 FROM crm_customer WHERE parent_customer_id = #{customerId}
                      AND tenant_id = #{tenantId} AND deleted = 0)
                UNION ALL SELECT 2, '联系人' FROM DUAL WHERE EXISTS (
                    SELECT 1 FROM crm_contact WHERE customer_id = #{customerId}
                      AND tenant_id = #{tenantId} AND deleted = 0)
                UNION ALL SELECT 3, '商机' FROM DUAL WHERE EXISTS (
                    SELECT 1 FROM crm_business WHERE customer_id = #{customerId}
                      AND tenant_id = #{tenantId} AND deleted = 0)
                UNION ALL SELECT 4, '合同' FROM DUAL WHERE EXISTS (
                    SELECT 1 FROM crm_contract WHERE customer_id = #{customerId}
                      AND tenant_id = #{tenantId} AND deleted = 0)
                UNION ALL SELECT 5, '已转换线索' FROM DUAL WHERE EXISTS (
                    SELECT 1 FROM crm_clue WHERE customer_id = #{customerId}
                      AND tenant_id = #{tenantId} AND deleted = 0)
                UNION ALL SELECT 6, '回款计划' FROM DUAL WHERE EXISTS (
                    SELECT 1 FROM crm_receivable_plan WHERE customer_id = #{customerId}
                      AND tenant_id = #{tenantId} AND deleted = 0)
                UNION ALL SELECT 7, '回款' FROM DUAL WHERE EXISTS (
                    SELECT 1 FROM crm_receivable WHERE customer_id = #{customerId}
                      AND tenant_id = #{tenantId} AND deleted = 0)
                UNION ALL SELECT 8, '发票' FROM DUAL WHERE EXISTS (
                    SELECT 1 FROM crm_invoice WHERE customer_id = #{customerId}
                      AND tenant_id = #{tenantId} AND deleted = 0)
                UNION ALL SELECT 9, '退款/冲销' FROM DUAL WHERE EXISTS (
                    SELECT 1 FROM crm_receivable_refund WHERE customer_id = #{customerId}
                      AND tenant_id = #{tenantId} AND deleted = 0)
                UNION ALL SELECT 10, '报销单' FROM DUAL WHERE EXISTS (
                    SELECT 1 FROM crm_reimbursement WHERE customer_id = #{customerId}
                      AND tenant_id = #{tenantId} AND deleted = 0)
                UNION ALL SELECT 11, '客服工单' FROM DUAL WHERE EXISTS (
                    SELECT 1 FROM crm_work_order WHERE customer_id = #{customerId}
                      AND tenant_id = #{tenantId} AND deleted = 0)
            ) references_found
            ORDER BY sort_no
            LIMIT 1
            """)
    String selectFirstReference(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);
}

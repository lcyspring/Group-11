package com.meession.etm.module.crm.service.customer;

import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomer360SummaryRespVO;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomer360Mapper;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Service
@Validated
public class CrmCustomer360ServiceImpl implements CrmCustomer360Service {

    @Resource
    private CrmCustomerService customerService;
    @Resource
    private CrmCustomer360Mapper customer360Mapper;

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#customerId",
            level = CrmPermissionLevelEnum.READ)
    public CrmCustomer360SummaryRespVO getSummary(Long customerId, Long userId,
                                                   boolean queryAllWorkOrders) {
        customerService.validateCustomer(customerId);
        CrmCustomer360SummaryRespVO summary = customer360Mapper.selectSummary(
                customerId, userId, queryAllWorkOrders);
        if (summary == null) {
            summary = new CrmCustomer360SummaryRespVO().setCustomerId(customerId);
        }
        normalize(summary);
        summary.setNetReceivableAmount(nonNegative(
                summary.getApprovedReceivableAmount().subtract(summary.getApprovedRefundAmount())));
        summary.setOutstandingReceivableAmount(nonNegative(
                summary.getContractAmount().subtract(summary.getNetReceivableAmount())));
        summary.setUninvoicedAmount(nonNegative(
                summary.getContractAmount().subtract(summary.getEffectiveInvoiceAmount())));
        return summary;
    }

    private static void normalize(CrmCustomer360SummaryRespVO summary) {
        summary.setContactCount(orZero(summary.getContactCount()))
                .setBusinessCount(orZero(summary.getBusinessCount()))
                .setMappedOrderCount(orZero(summary.getMappedOrderCount()))
                .setReceivablePlanCount(orZero(summary.getReceivablePlanCount()))
                .setReceivableCount(orZero(summary.getReceivableCount()))
                .setRefundCount(orZero(summary.getRefundCount()))
                .setInvoiceCount(orZero(summary.getInvoiceCount()))
                .setWorkOrderCount(orZero(summary.getWorkOrderCount()))
                .setTaskCount(orZero(summary.getTaskCount()))
                .setContractAttachmentCount(orZero(summary.getContractAttachmentCount()))
                .setContractAmount(orZero(summary.getContractAmount()))
                .setApprovedReceivableAmount(orZero(summary.getApprovedReceivableAmount()))
                .setApprovedRefundAmount(orZero(summary.getApprovedRefundAmount()))
                .setEffectiveInvoiceAmount(orZero(summary.getEffectiveInvoiceAmount()));
    }

    private static Long orZero(Long value) {
        return value == null ? 0L : value;
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        return value.signum() < 0 ? BigDecimal.ZERO : value;
    }
}

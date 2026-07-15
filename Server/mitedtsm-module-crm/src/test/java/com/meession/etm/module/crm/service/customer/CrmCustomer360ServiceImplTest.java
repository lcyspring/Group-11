package com.meession.etm.module.crm.service.customer;

import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomer360SummaryRespVO;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomer360Mapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CrmCustomer360ServiceImplTest {

    @Test
    void summaryUsesExplicitFinancialContractsAndNormalizesNulls() {
        AtomicBoolean customerValidated = new AtomicBoolean();
        CrmCustomer360ServiceImpl service = service(customerValidated,
                new CrmCustomer360SummaryRespVO().setCustomerId(7L)
                        .setContactCount(2L).setMappedOrderCount(3L)
                        .setContractAmount(new BigDecimal("1000.00"))
                        .setApprovedReceivableAmount(new BigDecimal("400.00"))
                        .setApprovedRefundAmount(new BigDecimal("50.00"))
                        .setEffectiveInvoiceAmount(new BigDecimal("250.00")));

        CrmCustomer360SummaryRespVO result = service.getSummary(7L, 9L, false);

        assertTrue(customerValidated.get());
        assertEquals(2L, result.getContactCount());
        assertEquals(0L, result.getBusinessCount());
        assertEquals(3L, result.getMappedOrderCount());
        assertEquals(0L, result.getRefundCount());
        assertEquals(new BigDecimal("350.00"), result.getNetReceivableAmount());
        assertEquals(new BigDecimal("650.00"), result.getOutstandingReceivableAmount());
        assertEquals(new BigDecimal("750.00"), result.getUninvoicedAmount());
        assertFalse(result.getTaskSupported());
    }

    @Test
    void summaryNeverReportsNegativeOutstandingBalances() {
        CrmCustomer360ServiceImpl service = service(new AtomicBoolean(),
                new CrmCustomer360SummaryRespVO().setCustomerId(8L)
                        .setContractAmount(new BigDecimal("100.00"))
                        .setApprovedReceivableAmount(new BigDecimal("120.00"))
                        .setApprovedRefundAmount(new BigDecimal("150.00"))
                        .setEffectiveInvoiceAmount(new BigDecimal("130.00")));

        CrmCustomer360SummaryRespVO result = service.getSummary(8L, 9L, true);

        assertEquals(BigDecimal.ZERO, result.getNetReceivableAmount());
        assertEquals(new BigDecimal("100.00"), result.getOutstandingReceivableAmount());
        assertEquals(BigDecimal.ZERO, result.getUninvoicedAmount());
    }

    @Test
    void summaryTreatsMissingRefundAggregateAsZero() {
        CrmCustomer360ServiceImpl service = service(new AtomicBoolean(),
                new CrmCustomer360SummaryRespVO().setCustomerId(9L)
                        .setContractAmount(new BigDecimal("100.00"))
                        .setApprovedReceivableAmount(new BigDecimal("40.00")));

        CrmCustomer360SummaryRespVO result = service.getSummary(9L, 9L, false);

        assertEquals(BigDecimal.ZERO, result.getApprovedRefundAmount());
        assertEquals(new BigDecimal("40.00"), result.getNetReceivableAmount());
        assertEquals(new BigDecimal("60.00"), result.getOutstandingReceivableAmount());
    }

    private static CrmCustomer360ServiceImpl service(AtomicBoolean customerValidated,
                                                      CrmCustomer360SummaryRespVO mapperResult) {
        CrmCustomer360ServiceImpl service = new CrmCustomer360ServiceImpl();
        CrmCustomerService customerService = proxy(CrmCustomerService.class, (proxy, method, args) -> {
            if (method.getName().equals("validateCustomer")) {
                assertEquals(mapperResult.getCustomerId(), args[0]);
                customerValidated.set(true);
                return null;
            }
            throw new AssertionError("未预期的客户 Service 方法: " + method.getName());
        });
        CrmCustomer360Mapper mapper = proxy(CrmCustomer360Mapper.class, (proxy, method, args) -> {
            if (method.getName().equals("selectSummary")) {
                assertEquals(mapperResult.getCustomerId(), args[0]);
                assertEquals(9L, args[1]);
                return mapperResult;
            }
            throw new AssertionError("未预期的客户 360 Mapper 方法: " + method.getName());
        });
        ReflectionTestUtils.setField(service, "customerService", customerService);
        ReflectionTestUtils.setField(service, "customer360Mapper", mapper);
        return service;
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}

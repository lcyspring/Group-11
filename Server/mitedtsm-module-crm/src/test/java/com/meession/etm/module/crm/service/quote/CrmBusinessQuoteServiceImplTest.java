package com.meession.etm.module.crm.service.quote;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.crm.controller.admin.business.vo.business.CrmBusinessSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import com.meession.etm.module.crm.dal.dataobject.product.CrmProductDO;
import com.meession.etm.module.crm.dal.dataobject.quote.CrmBusinessQuoteActionRecordDO;
import com.meession.etm.module.crm.dal.dataobject.quote.CrmBusinessQuoteDO;
import com.meession.etm.module.crm.dal.dataobject.quote.CrmBusinessQuoteItemDO;
import com.meession.etm.module.crm.dal.mysql.business.CrmBusinessMapper;
import com.meession.etm.module.crm.dal.mysql.quote.CrmBusinessQuoteActionRecordMapper;
import com.meession.etm.module.crm.dal.mysql.quote.CrmBusinessQuoteItemMapper;
import com.meession.etm.module.crm.dal.mysql.quote.CrmBusinessQuoteMapper;
import com.meession.etm.module.crm.enums.quote.CrmQuoteStatusEnum;
import com.meession.etm.module.crm.framework.quote.CrmQuoteProperties;
import com.meession.etm.module.crm.service.product.CrmProductService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.QUOTE_CURRENCY_UNSUPPORTED;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.QUOTE_ITEM_REQUIRED;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.QUOTE_TAX_RATE_UNSUPPORTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrmBusinessQuoteServiceImplTest {

    @Test
    void createDraftCalculatesCurrencyTaxAndImmutableProductSnapshot() {
        AtomicReference<CrmBusinessQuoteDO> savedQuote = new AtomicReference<>();
        AtomicReference<List<CrmBusinessQuoteItemDO>> savedItems = new AtomicReference<>();
        CrmBusinessQuoteServiceImpl service = service(savedQuote, savedItems);

        Long id = service.createInitialDraft(10L, request("USD", new BigDecimal("10"),
                product(1L, "90", 2, "13"), product(2L, "50", 1, "0")), 7L);

        assertEquals(100L, id);
        CrmBusinessQuoteDO quote = savedQuote.get();
        assertEquals(new BigDecimal("230.000000"), quote.getSubtotal());
        assertEquals(new BigDecimal("23.000000"), quote.getDiscountAmount());
        assertEquals(new BigDecimal("207.000000"), quote.getNetAmount());
        assertEquals(new BigDecimal("21.060000"), quote.getTaxAmount());
        assertEquals(new BigDecimal("228.060000"), quote.getGrossAmount());
        assertEquals(new BigDecimal("1642.032000"), quote.getBaseGrossAmount());
        CrmBusinessQuoteItemDO item = savedItems.get().get(0);
        assertEquals("产品一", item.getProductNameSnapshot());
        assertEquals("SKU-1", item.getProductNoSnapshot());
        assertEquals(3, item.getProductVersionSnapshot());
        assertEquals(new BigDecimal("100.000000"), item.getListPrice());
    }

    @Test
    void createDraftRejectsCurrencyNotInYaml() {
        CrmBusinessQuoteServiceImpl service = service(new AtomicReference<>(), new AtomicReference<>());
        ServiceException ex = assertThrows(ServiceException.class, () -> service.createInitialDraft(10L,
                request("JPY", BigDecimal.ZERO, product(1L, "90", 1, "13")), 7L));
        assertEquals(QUOTE_CURRENCY_UNSUPPORTED.getCode(), ex.getCode());
    }

    @Test
    void createDraftRejectsTaxRateNotInYaml() {
        CrmBusinessQuoteServiceImpl service = service(new AtomicReference<>(), new AtomicReference<>());
        ServiceException ex = assertThrows(ServiceException.class, () -> service.createInitialDraft(10L,
                request("CNY", BigDecimal.ZERO, product(1L, "90", 1, "7")), 7L));
        assertEquals(QUOTE_TAX_RATE_UNSUPPORTED.getCode(), ex.getCode());
    }

    @Test
    void lockRejectsEmptyDraft() {
        CrmBusinessQuoteServiceImpl service = bareService();
        CrmBusinessQuoteDO draft = quote(100L, 1, CrmQuoteStatusEnum.DRAFT.getStatus());
        ReflectionTestUtils.setField(service, "businessMapper", proxy(CrmBusinessMapper.class,
                (proxy, method, args) -> new CrmBusinessDO().setId(10L)));
        ReflectionTestUtils.setField(service, "quoteMapper", proxy(CrmBusinessQuoteMapper.class,
                (proxy, method, args) -> draft));
        ReflectionTestUtils.setField(service, "itemMapper", proxy(CrmBusinessQuoteItemMapper.class,
                (proxy, method, args) -> List.of()));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.lockQuote(10L, "客户确认", 7L));
        assertEquals(QUOTE_ITEM_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void reopenSupersedesLockedVersionAndClonesItems() {
        CrmBusinessQuoteServiceImpl service = bareService();
        CrmBusinessQuoteDO old = quote(100L, 1, CrmQuoteStatusEnum.LOCKED.getStatus());
        CrmBusinessQuoteItemDO oldItem = new CrmBusinessQuoteItemDO().setId(11L).setQuoteId(100L)
                .setProductId(1L).setProductNameSnapshot("历史产品").setProductNoSnapshot("SKU-1")
                .setProductVersionSnapshot(3).setListPrice(BigDecimal.TEN).setBusinessPrice(BigDecimal.TEN)
                .setCount(BigDecimal.ONE).setTaxRatePercent(BigDecimal.ZERO).setLineSubtotal(BigDecimal.TEN)
                .setLineDiscountAmount(BigDecimal.ZERO).setNetAmount(BigDecimal.TEN)
                .setTaxAmount(BigDecimal.ZERO).setGrossAmount(BigDecimal.TEN);
        AtomicReference<List<CrmBusinessQuoteItemDO>> cloned = new AtomicReference<>();
        ReflectionTestUtils.setField(service, "businessMapper", proxy(CrmBusinessMapper.class,
                (proxy, method, args) -> new CrmBusinessDO().setId(10L)));
        ReflectionTestUtils.setField(service, "quoteMapper", proxy(CrmBusinessQuoteMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectLatestForUpdate" -> old;
                    case "supersedeLocked" -> 1;
                    case "insert" -> { ((CrmBusinessQuoteDO) args[0]).setId(101L); yield 1; }
                    default -> throw new AssertionError(method.getName());
                }));
        ReflectionTestUtils.setField(service, "itemMapper", proxy(CrmBusinessQuoteItemMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectByQuoteId" -> List.of(oldItem);
                    case "insertBatch" -> { cloned.set(new ArrayList<>((List<CrmBusinessQuoteItemDO>) args[0])); yield true; }
                    default -> throw new AssertionError(method.getName());
                }));
        ReflectionTestUtils.setField(service, "actionMapper", proxy(CrmBusinessQuoteActionRecordMapper.class,
                (proxy, method, args) -> 1));

        CrmBusinessQuoteDO reopened = service.reopenQuote(10L, "客户修改采购范围", 7L);

        assertEquals(2, reopened.getVersionNo());
        assertEquals(CrmQuoteStatusEnum.DRAFT.getStatus(), reopened.getStatus());
        assertEquals(100L, reopened.getSourceQuoteId());
        assertEquals(101L, cloned.get().get(0).getQuoteId());
        assertNotSame(oldItem, cloned.get().get(0));
    }

    @Test
    void terminateCurrentFreezesDraftAndRecordsTerminalState() {
        CrmBusinessQuoteServiceImpl service = bareService();
        CrmBusinessQuoteDO draft = quote(100L, 1, CrmQuoteStatusEnum.DRAFT.getStatus());
        AtomicReference<CrmBusinessQuoteActionRecordDO> action = new AtomicReference<>();
        ReflectionTestUtils.setField(service, "quoteMapper", proxy(CrmBusinessQuoteMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectLatestForUpdate" -> draft;
                    case "terminateCurrent" -> 1;
                    default -> throw new AssertionError(method.getName());
                }));
        ReflectionTestUtils.setField(service, "actionMapper", proxy(CrmBusinessQuoteActionRecordMapper.class,
                (proxy, method, args) -> {
                    action.set((CrmBusinessQuoteActionRecordDO) args[0]);
                    return 1;
                }));

        service.terminateCurrent(10L, "客户预算取消，商机正式输单", 7L);

        assertEquals(CrmQuoteStatusEnum.DRAFT.getStatus(), action.get().getFromStatus());
        assertEquals(CrmQuoteStatusEnum.TERMINATED.getStatus(), action.get().getToStatus());
        assertEquals("客户预算取消，商机正式输单", action.get().getRemark());
    }

    private static CrmBusinessQuoteServiceImpl service(AtomicReference<CrmBusinessQuoteDO> quote,
                                                        AtomicReference<List<CrmBusinessQuoteItemDO>> items) {
        CrmBusinessQuoteServiceImpl service = bareService();
        ReflectionTestUtils.setField(service, "quoteMapper", proxy(CrmBusinessQuoteMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectLatest" -> null;
                    case "insert" -> { CrmBusinessQuoteDO value = (CrmBusinessQuoteDO) args[0]; value.setId(100L); quote.set(value); yield 1; }
                    default -> throw new AssertionError(method.getName());
                }));
        ReflectionTestUtils.setField(service, "itemMapper", proxy(CrmBusinessQuoteItemMapper.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "deleteByQuoteId" -> null;
                    case "insertBatch" -> { items.set(new ArrayList<>((List<CrmBusinessQuoteItemDO>) args[0])); yield true; }
                    default -> throw new AssertionError(method.getName());
                }));
        ReflectionTestUtils.setField(service, "actionMapper", proxy(CrmBusinessQuoteActionRecordMapper.class,
                (proxy, method, args) -> 1));
        ReflectionTestUtils.setField(service, "productService", proxy(CrmProductService.class,
                (proxy, method, args) -> List.of(
                        new CrmProductDO().setId(1L).setName("产品一").setNo("SKU-1").setUnit(1)
                                .setCategoryId(20L).setPrice(new BigDecimal("100")).setVersion(3),
                        new CrmProductDO().setId(2L).setName("产品二").setNo("SKU-2").setUnit(2)
                                .setCategoryId(21L).setPrice(new BigDecimal("50")).setVersion(5))));
        return service;
    }

    private static CrmBusinessQuoteServiceImpl bareService() {
        CrmBusinessQuoteServiceImpl service = new CrmBusinessQuoteServiceImpl();
        ReflectionTestUtils.setField(service, "properties",
                com.meession.etm.module.crm.framework.quote.CrmQuotePropertiesTest.validProperties());
        return service;
    }

    private static CrmBusinessSaveReqVO request(String currency, BigDecimal discount,
                                                CrmBusinessSaveReqVO.BusinessProduct... products) {
        return new CrmBusinessSaveReqVO().setId(10L).setCurrencyCode(currency)
                .setDiscountPercent(discount).setProducts(List.of(products));
    }

    private static CrmBusinessSaveReqVO.BusinessProduct product(Long id, String price, int count, String tax) {
        return new CrmBusinessSaveReqVO.BusinessProduct().setProductId(id)
                .setBusinessPrice(new BigDecimal(price)).setProductPrice(new BigDecimal("999"))
                .setCount(new BigDecimal(String.valueOf(count))).setTaxRatePercent(new BigDecimal(tax));
    }

    private static CrmBusinessQuoteDO quote(Long id, int versionNo, int status) {
        return new CrmBusinessQuoteDO().setId(id).setBusinessId(10L).setVersionNo(versionNo).setStatus(status)
                .setCurrencyCode("CNY").setBaseCurrencyCode("CNY").setExchangeRateToBase(BigDecimal.ONE)
                .setDiscountPercent(BigDecimal.ZERO).setSubtotal(BigDecimal.TEN).setDiscountAmount(BigDecimal.ZERO)
                .setNetAmount(BigDecimal.TEN).setTaxAmount(BigDecimal.ZERO).setGrossAmount(BigDecimal.TEN)
                .setBaseGrossAmount(BigDecimal.TEN).setVersion(0);
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}

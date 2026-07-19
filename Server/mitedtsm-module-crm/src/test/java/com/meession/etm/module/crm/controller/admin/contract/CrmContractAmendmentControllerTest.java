package com.meession.etm.module.crm.controller.admin.contract;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.util.json.JsonUtils;
import com.meession.etm.module.crm.controller.admin.contract.vo.amendment.CrmContractAmendmentCommandReqVO;
import com.meession.etm.module.crm.controller.admin.contract.vo.amendment.CrmContractAmendmentRespVO;
import com.meession.etm.module.crm.controller.admin.contract.vo.amendment.CrmContractAmendmentSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractAmendmentDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractProductDO;
import com.meession.etm.module.crm.service.contract.CrmContractAmendmentService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmContractAmendmentControllerTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Mock
    private CrmContractAmendmentService service;
    @InjectMocks
    private CrmContractAmendmentController controller;

    @Test
    void commandsAndQueriesExposeSeparateSystemPermissionBoundaries() throws Exception {
        assertPermission("create", "crm:contract:amendment", CrmContractAmendmentSaveReqVO.class);
        assertPermission("update", "crm:contract:amendment", CrmContractAmendmentSaveReqVO.class);
        assertPermission("submit", "crm:contract:amendment", CrmContractAmendmentCommandReqVO.class);
        assertPermission("get", "crm:contract:query", Long.class, Long.class);
        assertPermission("list", "crm:contract:query", Long.class);
    }

    @Test
    void saveRequestValidatesNestedProductsAndRequiredBusinessIdentity() {
        CrmContractAmendmentSaveReqVO request = new CrmContractAmendmentSaveReqVO()
                .setContractId(7L).setClientRequestId("req-1").setTitle("补充协议")
                .setReason("扩展范围").setContractName("合同 V2").setDiscountPercent(BigDecimal.ZERO)
                .setProducts(List.of(new CrmContractAmendmentSaveReqVO.Product()
                        .setProductId(41L).setContractPrice(BigDecimal.ONE).setCount(BigDecimal.ONE)));
        assertTrue(validator.validate(request).isEmpty());

        request.setContractId(null).setClientRequestId(" ").setDiscountPercent(new BigDecimal("101"));
        request.getProducts().get(0).setContractPrice(BigDecimal.ZERO).setCount(BigDecimal.ZERO);
        Set<String> invalidPaths = validator.validate(request).stream()
                .map(item -> item.getPropertyPath().toString()).collect(Collectors.toSet());
        assertTrue(invalidPaths.contains("contractId"));
        assertTrue(invalidPaths.contains("clientRequestId"));
        assertTrue(invalidPaths.contains("discountPercent"));
        assertTrue(invalidPaths.contains("products[0].contractPrice"));
        assertTrue(invalidPaths.contains("products[0].count"));
        assertEquals(2, validator.validate(new CrmContractAmendmentCommandReqVO()).size());
    }

    @Test
    void getReturnsEditableProjectionWithoutLeakingRawSnapshots() {
        CrmContractDO after = new CrmContractDO().setName("合同 V2")
                .setStartTime(LocalDateTime.of(2026, 8, 1, 0, 0)).setDiscountPercent(new BigDecimal("5"));
        CrmContractProductDO product = new CrmContractProductDO().setId(31L).setProductId(41L)
                .setProductNameSnapshot("产品快照").setContractPrice(new BigDecimal("100"))
                .setCount(BigDecimal.ONE).setTotalPrice(new BigDecimal("100"));
        CrmContractAmendmentDO amendment = new CrmContractAmendmentDO().setId(11L).setContractId(7L)
                .setNo("BC202607-0001").setAfterContractSnapshot(JsonUtils.toJsonString(after))
                .setAfterProductSnapshot(JsonUtils.toJsonString(List.of(product)));
        when(service.getAmendment(7L, 11L)).thenReturn(amendment);

        CommonResult<CrmContractAmendmentRespVO> result = controller.get(7L, 11L);
        String json = JsonUtils.toJsonString(result);

        assertEquals("合同 V2", result.getData().getContractName());
        assertEquals("产品快照", result.getData().getProducts().get(0).getProductNameSnapshot());
        assertFalse(json.contains("afterContractSnapshot"));
        assertFalse(json.contains("afterProductSnapshot"));
    }

    private static void assertPermission(String methodName, String permission, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = CrmContractAmendmentController.class.getMethod(methodName, parameterTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertTrue(annotation.value().contains(permission));
    }
}

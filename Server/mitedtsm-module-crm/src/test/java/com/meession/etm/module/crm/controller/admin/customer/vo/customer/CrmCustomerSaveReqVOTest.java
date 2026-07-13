package com.meession.etm.module.crm.controller.admin.customer.vo.customer;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmCustomerSaveReqVOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void customerNameAcceptsOneHundredCharacters() {
        CrmCustomerSaveReqVO reqVO = new CrmCustomerSaveReqVO()
                .setName("客".repeat(100))
                .setOwnerUserId(1L);

        assertEquals(0, validator.validate(reqVO).size());
    }

    @Test
    void customerNameRejectsMoreThanOneHundredCharacters() {
        CrmCustomerSaveReqVO reqVO = new CrmCustomerSaveReqVO()
                .setName("客".repeat(101))
                .setOwnerUserId(1L);

        assertEquals("客户名称长度不能超过 100 个字符",
                validator.validate(reqVO).iterator().next().getMessage());
    }

}

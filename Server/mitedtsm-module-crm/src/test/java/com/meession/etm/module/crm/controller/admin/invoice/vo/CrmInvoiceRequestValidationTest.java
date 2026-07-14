package com.meession.etm.module.crm.controller.admin.invoice.vo;

import com.meession.etm.module.crm.enums.invoice.CrmInvoiceTypeEnum;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmInvoiceRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void draftRejectsZeroAmountAndInvalidEmail() {
        CrmInvoiceCreateReqVO request = validDraft();
        request.setAmount(BigDecimal.ZERO).setEmail("not-an-email");
        assertEquals(2, validator.validate(request).size());
    }

    @Test
    void createAndUpdateExposeDifferentImmutableIdentityFields() {
        assertEquals(0, validator.validate(validDraft()).size());
        CrmInvoiceUpdateReqVO update = new CrmInvoiceUpdateReqVO();
        update.setId(10L);
        update.setHandlerUserId(2L).setType(1).setAmount(new BigDecimal("0.01"))
                .setTitle("客户").setContent("服务费");
        assertEquals(0, validator.validate(update).size());
    }

    @Test
    void lifecycleCommandsRequireFiscalIdentityAndReason() {
        CrmInvoiceIssueReqVO issue = new CrmInvoiceIssueReqVO().setId(10L).setInvoiceNo("B-1")
                .setInvoiceDate(LocalDateTime.now()).setHandlerUserId(2L);
        assertEquals(0, validator.validate(issue).size());
        CrmInvoiceRedFlushReqVO red = new CrmInvoiceRedFlushReqVO().setOriginalInvoiceId(10L)
                .setAmount(new BigDecimal("1.00")).setInvoiceNo("R-1").setInvoiceDate(LocalDateTime.now())
                .setHandlerUserId(2L).setReason("退货");
        assertEquals(0, validator.validate(red).size());
        assertEquals(1, validator.validate(new CrmInvoiceVoidReqVO().setId(10L).setReason(" ")).size());
    }

    private static CrmInvoiceCreateReqVO validDraft() {
        CrmInvoiceCreateReqVO request = new CrmInvoiceCreateReqVO();
        request.setContractId(20L);
        request.setHandlerUserId(2L).setType(CrmInvoiceTypeEnum.VAT_ORDINARY.getType())
                .setAmount(new BigDecimal("0.01")).setTitle("客户").setContent("服务费");
        return request;
    }
}

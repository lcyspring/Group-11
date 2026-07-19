package com.meession.etm.module.crm.service.marketing;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.crm.controller.admin.marketing.vo.CrmMarketingBroadcastSaveReqVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.CrmMarketingConsentSaveReqVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.CrmCustomerCarePlanSaveReqVO;
import com.meession.etm.module.crm.framework.marketing.CrmMarketingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrmMarketingOutreachValidationTest {
    @Test
    void broadcastRequiresChannelTemplate() {
        CrmMarketingOutreachService service = new CrmMarketingOutreachService();
        CrmMarketingBroadcastSaveReqVO request = new CrmMarketingBroadcastSaveReqVO().setName("x").setChannel(1);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveBroadcast(request, 1L));
        assertEquals(MARKETING_TEMPLATE_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void consentRejectsUnknownChannelBeforeDatabaseAccess() {
        CrmMarketingOutreachService service = new CrmMarketingOutreachService();
        CrmMarketingConsentSaveReqVO request = new CrmMarketingConsentSaveReqVO().setCustomerId(1L).setChannel(9).setStatus(1);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveConsent(request, 1L));
        assertEquals(MARKETING_CHANNEL_INVALID.getCode(), ex.getCode());
    }

    @Test
    void carePlanRejectsMalformedDate() {
        CrmCustomerCareServiceImpl service = new CrmCustomerCareServiceImpl();
        CrmCustomerCarePlanSaveReqVO request = new CrmCustomerCarePlanSaveReqVO().setCode("b").setName("生日")
                .setRuleType(2).setEventMonthDay("tomorrow").setChannel(1).setSmsTemplateCode("sms").setEnabled(false);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.savePlan(request));
        assertEquals(MARKETING_CARE_RULE_INVALID.getCode(), ex.getCode());
    }
}

package com.meession.etm.module.system.api.mail;

import com.meession.etm.framework.common.enums.CommonStatusEnum;
import com.meession.etm.module.system.dal.dataobject.mail.MailAccountDO;
import com.meession.etm.module.system.dal.dataobject.mail.MailTemplateDO;
import com.meession.etm.module.system.service.mail.MailAccountService;
import com.meession.etm.module.system.service.mail.MailLogService;
import com.meession.etm.module.system.service.mail.MailSendService;
import com.meession.etm.module.system.service.mail.MailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailSendApiImplReadinessTest {
    @Mock MailSendService mailSendService;
    @Mock MailLogService mailLogService;
    @Mock MailTemplateService mailTemplateService;
    @Mock MailAccountService mailAccountService;
    private MailSendApiImpl api;

    @BeforeEach
    void setUp() {
        api = new MailSendApiImpl();
        ReflectionTestUtils.setField(api, "mailSendService", mailSendService);
        ReflectionTestUtils.setField(api, "mailLogService", mailLogService);
        ReflectionTestUtils.setField(api, "mailTemplateService", mailTemplateService);
        ReflectionTestUtils.setField(api, "mailAccountService", mailAccountService);
    }

    @Test
    void blankTemplateIsNotReadyWithoutQueryingCache() {
        var result = api.getMailTemplateReadiness(" ", Map.of());
        assertFalse(result.isTemplateExists());
    }

    @Test
    void completeTemplateAndSmtpAccountIsReady() {
        MailTemplateDO template = new MailTemplateDO().setCode("crm-mail").setAccountId(9L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setParams(List.of("name"));
        MailAccountDO account = new MailAccountDO().setMail("from@example.com").setHost("smtp.example.com")
                .setPort(465).setUsername("smtp-user");
        when(mailTemplateService.getMailTemplateByCodeFromCache("crm-mail")).thenReturn(template);
        when(mailAccountService.getMailAccountFromCache(9L)).thenReturn(account);

        var result = api.getMailTemplateReadiness("crm-mail", Map.of("name", "Alice"));
        assertTrue(result.isTemplateExists());
        assertTrue(result.isTemplateEnabled());
        assertTrue(result.isAccountConfigured());
        assertTrue(result.getMissingParams().isEmpty());
    }

    @Test
    void missingSmtpAndTemplateParamsAreReported() {
        MailTemplateDO template = new MailTemplateDO().setCode("crm-mail").setAccountId(9L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setParams(List.of("name", "link"));
        when(mailTemplateService.getMailTemplateByCodeFromCache("crm-mail")).thenReturn(template);
        when(mailAccountService.getMailAccountFromCache(9L)).thenReturn(new MailAccountDO().setMail("from@example.com"));

        var result = api.getMailTemplateReadiness("crm-mail", Map.of("name", "Alice"));
        assertFalse(result.isAccountConfigured());
        assertEquals(List.of("link"), result.getMissingParams());
    }
}

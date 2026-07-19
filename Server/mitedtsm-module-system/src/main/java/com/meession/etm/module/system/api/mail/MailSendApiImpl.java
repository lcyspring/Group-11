package com.meession.etm.module.system.api.mail;

import com.meession.etm.module.system.api.mail.dto.MailSendSingleToUserReqDTO;
import com.meession.etm.module.system.api.mail.dto.MailSendStatusRespDTO;
import com.meession.etm.module.system.api.mail.dto.MailTemplateReadinessRespDTO;
import com.meession.etm.framework.common.enums.CommonStatusEnum;
import com.meession.etm.module.system.dal.dataobject.mail.MailAccountDO;
import com.meession.etm.module.system.dal.dataobject.mail.MailTemplateDO;
import com.meession.etm.module.system.dal.dataobject.mail.MailLogDO;
import com.meession.etm.module.system.service.mail.MailAccountService;
import com.meession.etm.module.system.service.mail.MailLogService;
import com.meession.etm.module.system.service.mail.MailSendService;
import com.meession.etm.module.system.service.mail.MailTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;

/**
 * 邮件发送 API 实现类
 *
 * @author wangjingyi
 */
@Service
@Validated
public class MailSendApiImpl implements MailSendApi {

    @Resource
    private MailSendService mailSendService;
    @Resource
    private MailLogService mailLogService;
    @Resource
    private MailTemplateService mailTemplateService;
    @Resource
    private MailAccountService mailAccountService;

    @Override
    public Long sendSingleMailToAdmin(MailSendSingleToUserReqDTO reqDTO) {
        return mailSendService.sendSingleMailToAdmin(reqDTO.getUserId(),
                reqDTO.getToMails(), reqDTO.getCcMails(), reqDTO.getBccMails(),
                reqDTO.getTemplateCode(), reqDTO.getTemplateParams(), reqDTO.getAttachments());
    }

    @Override
    public Long sendSingleMailToMember(MailSendSingleToUserReqDTO reqDTO) {
        return mailSendService.sendSingleMailToMember(reqDTO.getUserId(),
                reqDTO.getToMails(), reqDTO.getCcMails(), reqDTO.getBccMails(),
                reqDTO.getTemplateCode(), reqDTO.getTemplateParams(), reqDTO.getAttachments());
    }

    @Override
    public MailSendStatusRespDTO getMailSendStatus(Long logId) {
        MailLogDO log = mailLogService.getMailLog(logId);
        if (log == null) {
            return null;
        }
        return new MailSendStatusRespDTO().setLogId(log.getId()).setSendStatus(log.getSendStatus())
                .setSendTime(log.getSendTime()).setSendMessageId(log.getSendMessageId())
                .setSendException(log.getSendException());
    }

    @Override
    public MailTemplateReadinessRespDTO getMailTemplateReadiness(String templateCode,
                                                                  java.util.Map<String, Object> templateParams) {
        MailTemplateReadinessRespDTO result = new MailTemplateReadinessRespDTO();
        if (templateCode == null || templateCode.isBlank()) {
            return result;
        }
        MailTemplateDO template = mailTemplateService.getMailTemplateByCodeFromCache(templateCode);
        if (template == null) return result;
        result.setTemplateExists(true)
                .setTemplateEnabled(CommonStatusEnum.ENABLE.getStatus().equals(template.getStatus()));
        MailAccountDO account = mailAccountService.getMailAccountFromCache(template.getAccountId());
        result.setAccountConfigured(account != null && account.getMail() != null && !account.getMail().isBlank()
                && account.getHost() != null && !account.getHost().isBlank()
                && account.getPort() != null && account.getPort() > 0
                && account.getUsername() != null && !account.getUsername().isBlank());
        java.util.Map<String, Object> params = templateParams == null ? java.util.Map.of() : templateParams;
        result.setMissingParams(java.util.Optional.ofNullable(template.getParams()).orElse(java.util.List.of())
                .stream().filter(param -> !params.containsKey(param)).toList());
        return result;
    }

}

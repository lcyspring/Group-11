package com.meession.etm.module.system.api.mail;

import com.meession.etm.module.system.api.mail.dto.MailSendSingleToUserReqDTO;
import com.meession.etm.module.system.api.mail.dto.MailSendStatusRespDTO;
import com.meession.etm.module.system.dal.dataobject.mail.MailLogDO;
import com.meession.etm.module.system.service.mail.MailLogService;
import com.meession.etm.module.system.service.mail.MailSendService;
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

}

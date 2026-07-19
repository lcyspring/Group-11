package com.meession.etm.module.system.api.sms;

import com.meession.etm.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import com.meession.etm.module.system.api.sms.dto.SmsSendStatusRespDTO;
import com.meession.etm.module.system.dal.dataobject.sms.SmsLogDO;
import com.meession.etm.module.system.service.sms.SmsLogService;
import com.meession.etm.module.system.service.sms.SmsSendService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;

/**
 * 短信发送 API 接口
 *
 * @author 密讯
 */
@Service
@Validated
public class SmsSendApiImpl implements SmsSendApi {

    @Resource
    private SmsSendService smsSendService;
    @Resource
    private SmsLogService smsLogService;

    @Override
    public Long sendSingleSmsToAdmin(SmsSendSingleToUserReqDTO reqDTO) {
        return smsSendService.sendSingleSmsToAdmin(reqDTO.getMobile(), reqDTO.getUserId(),
                reqDTO.getTemplateCode(), reqDTO.getTemplateParams());
    }

    @Override
    public Long sendSingleSmsToMember(SmsSendSingleToUserReqDTO reqDTO) {
        return smsSendService.sendSingleSmsToMember(reqDTO.getMobile(), reqDTO.getUserId(),
                reqDTO.getTemplateCode(), reqDTO.getTemplateParams());
    }

    @Override
    public SmsSendStatusRespDTO getSmsSendStatus(Long logId) {
        SmsLogDO log = smsLogService.getSmsLog(logId);
        if (log == null) {
            return null;
        }
        return new SmsSendStatusRespDTO().setLogId(log.getId())
                .setSendStatus(log.getSendStatus()).setReceiveStatus(log.getReceiveStatus())
                .setSendTime(log.getSendTime()).setReceiveTime(log.getReceiveTime())
                .setSendMessage(log.getApiSendMsg()).setReceiveMessage(log.getApiReceiveMsg());
    }

}

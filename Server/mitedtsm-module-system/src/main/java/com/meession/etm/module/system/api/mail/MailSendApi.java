package com.meession.etm.module.system.api.mail;

import com.meession.etm.module.system.api.mail.dto.MailSendSingleToUserReqDTO;
import com.meession.etm.module.system.api.mail.dto.MailSendStatusRespDTO;

import jakarta.validation.Valid;

/**
 * 邮箱发送 API 接口
 *
 * @author 密讯
 */
public interface MailSendApi {

    /**
     * 发送单条邮箱给 Admin 用户
     *
     * 在 mail 为空时，使用 userId 加载对应 Admin 的邮箱
     *
     * @param reqDTO 发送请求
     * @return 发送日志编号
     */
    Long sendSingleMailToAdmin(@Valid MailSendSingleToUserReqDTO reqDTO);

    /**
     * 发送单条邮箱给 Member 用户
     *
     * 在 mail 为空时，使用 userId 加载对应 Member 的邮箱
     *
     * @param reqDTO 发送请求
     * @return 发送日志编号
     */
    Long sendSingleMailToMember(@Valid MailSendSingleToUserReqDTO reqDTO);

    /** 获得邮件异步发送状态；日志不存在时返回 {@code null}。 */
    MailSendStatusRespDTO getMailSendStatus(Long logId);

}

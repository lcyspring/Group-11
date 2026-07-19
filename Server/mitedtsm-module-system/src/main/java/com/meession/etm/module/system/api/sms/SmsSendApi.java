package com.meession.etm.module.system.api.sms;

import com.meession.etm.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import com.meession.etm.module.system.api.sms.dto.SmsSendStatusRespDTO;

import jakarta.validation.Valid;

/**
 * 短信发送 API 接口
 *
 * @author 密讯
 */
public interface SmsSendApi {

    /**
     * 发送单条短信给 Admin 用户
     *
     * 在 mobile 为空时，使用 userId 加载对应 Admin 的手机号
     *
     * @param reqDTO 发送请求
     * @return 发送日志编号
     */
    Long sendSingleSmsToAdmin(@Valid SmsSendSingleToUserReqDTO reqDTO);

    /**
     * 发送单条短信给 Member 用户
     *
     * 在 mobile 为空时，使用 userId 加载对应 Member 的手机号
     *
     * @param reqDTO 发送请求
     * @return 发送日志编号
     */
    Long sendSingleSmsToMember(@Valid SmsSendSingleToUserReqDTO reqDTO);

    /**
     * 获得短信发送及回执状态，仅供内部业务按发送日志编号回收结果。
     *
     * @param logId 发送日志编号
     * @return 状态快照；日志不存在时返回 {@code null}
     */
    SmsSendStatusRespDTO getSmsSendStatus(Long logId);

}

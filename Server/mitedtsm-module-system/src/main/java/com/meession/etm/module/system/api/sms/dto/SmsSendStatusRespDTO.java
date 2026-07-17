package com.meession.etm.module.system.api.sms.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 短信发送日志的内部状态快照。 */
@Data
public class SmsSendStatusRespDTO {
    private Long logId;
    private Integer sendStatus;
    private Integer receiveStatus;
    private LocalDateTime sendTime;
    private LocalDateTime receiveTime;
    private String sendMessage;
    private String receiveMessage;
}

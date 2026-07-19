package com.meession.etm.module.system.api.mail.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 邮件发送日志的内部状态快照。 */
@Data
public class MailSendStatusRespDTO {
    private Long logId;
    private Integer sendStatus;
    private LocalDateTime sendTime;
    private String sendMessageId;
    private String sendException;
}

package com.meession.etm.module.crm.dal.dataobject.activity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("crm_sms_record")
@KeySequence("crm_sms_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmSmsRecordDO extends BaseDO {
    @TableId
    private Long id;
    private Integer bizType;
    private Long bizId;
    private Long sourceClueId;
    private Long contactId;
    private Integer direction;
    private Integer status;
    private String mobile;
    private String content;
    private Long systemSmsLogId;
    private String externalMessageId;
    private String failureReason;
    private LocalDateTime occurredTime;
    private Long operatorUserId;
}

package com.meession.etm.module.crm.dal.dataobject.activity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("crm_call_record")
@KeySequence("crm_call_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmCallRecordDO extends BaseDO {
    @TableId
    private Long id;
    private Integer bizType;
    private Long bizId;
    private Long sourceClueId;
    private Long contactId;
    private Integer direction;
    private Integer status;
    private String phone;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationSeconds;
    private String recordingUrl;
    private String summary;
    private Long operatorUserId;
}

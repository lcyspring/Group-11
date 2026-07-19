package com.meession.etm.module.crm.dal.dataobject.activity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("crm_clue_conversion_record")
@KeySequence("crm_clue_conversion_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmClueConversionRecordDO extends BaseDO {
    @TableId
    private Long id;
    private Long clueId;
    private Long customerId;
    private Long primaryContactId;
    private Integer followUpCount;
    private Integer taskCount;
    private Integer callCount;
    private Integer smsCount;
    private Long operatorUserId;
    private LocalDateTime convertedAt;
}

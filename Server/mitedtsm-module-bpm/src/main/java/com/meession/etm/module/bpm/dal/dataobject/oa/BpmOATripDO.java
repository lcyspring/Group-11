package com.meession.etm.module.bpm.dal.dataobject.oa;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@TableName(value = "bpm_oa_trip", autoResultMap = true)
@KeySequence("bpm_oa_trip_seq")
@Data
public class BpmOATripDO extends BaseDO {

    @TableId
    private Long id;
    private Long userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal days;
    private String destination;
    private String reason;
    private BigDecimal estimatedExpense;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> companionUserIds;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> attachmentUrls;
    private Integer status;
    private String processInstanceId;
    private LocalDateTime approvalTime;
}

package com.meession.etm.module.bpm.dal.dataobject.oa;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("bpm_oa_business_trip")
@KeySequence("bpm_oa_business_trip_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BpmOABusinessTripDO extends BaseDO {

    @TableId
    private Long id;

    private Long userId;

    private String destination;

    private String reason;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long day;

    private BigDecimal budget;

    private Integer status;

    private String processInstanceId;

}
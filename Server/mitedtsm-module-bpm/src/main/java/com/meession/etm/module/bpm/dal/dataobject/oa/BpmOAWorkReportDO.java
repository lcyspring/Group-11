package com.meession.etm.module.bpm.dal.dataobject.oa;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@TableName("bpm_oa_work_report")
@KeySequence("bpm_oa_work_report_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BpmOAWorkReportDO extends BaseDO {

    @TableId
    private Long id;

    private Long userId;

    private String type;

    private LocalDate reportDate;

    private String content;

    private String tomorrowPlan;

    private String problems;

    private Integer status;

}
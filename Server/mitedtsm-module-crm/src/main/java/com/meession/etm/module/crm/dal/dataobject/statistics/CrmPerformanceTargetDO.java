package com.meession.etm.module.crm.dal.dataobject.statistics;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * CRM 月度业绩目标 DO。
 *
 * 一条记录只保存一个月份，年度和季度目标均由月度目标汇总，避免重复存储产生口径漂移。
 */
@TableName("crm_performance_target")
@KeySequence("crm_performance_target_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmPerformanceTargetDO extends BaseDO {

    @TableId
    private Long id;
    private Integer scopeType;
    private Long scopeId;
    private Integer targetYear;
    private Integer targetMonth;
    private Integer targetType;
    private BigDecimal targetValue;

}

package com.meession.etm.module.crm.dal.dataobject.workorder;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@TableName("crm_work_order_holiday")
@KeySequence("crm_work_order_holiday_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmWorkOrderHolidayDO extends BaseDO {
    @TableId
    private Long id;
    private LocalDate holidayDate;
    private String name;
    private Boolean workingDay;
}

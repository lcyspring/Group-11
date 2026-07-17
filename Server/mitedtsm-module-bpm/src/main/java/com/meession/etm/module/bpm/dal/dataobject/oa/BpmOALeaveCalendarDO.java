package com.meession.etm.module.bpm.dal.dataobject.oa;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@TableName("bpm_oa_leave_calendar")
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmOALeaveCalendarDO extends TenantBaseDO {
    @TableId
    private Long id;
    private LocalDate calendarDate;
    private Boolean workday;
    private String name;
}

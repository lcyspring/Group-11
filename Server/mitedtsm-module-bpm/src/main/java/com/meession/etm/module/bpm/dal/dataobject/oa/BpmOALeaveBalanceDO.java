package com.meession.etm.module.bpm.dal.dataobject.oa;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("bpm_oa_leave_balance")
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmOALeaveBalanceDO extends TenantBaseDO {
    @TableId
    private Long id;
    private Long userId;
    private Integer leaveType;
    private Integer balanceYear;
    private Long totalDays;
    private Long reservedDays;
    private Long usedDays;

    public long availableDays() {
        return totalDays - reservedDays - usedDays;
    }
}

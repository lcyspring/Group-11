package com.meession.etm.module.crm.enums.workorder;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmWorkOrderActionTypeEnum {
    CREATE(1, "创建"), UPDATE(2, "修改"), START(3, "开始处理"), RETURN(4, "退回"),
    RESUBMIT(5, "重新提交"), COMPLETE(6, "完结");

    private final Integer type;
    private final String name;
}

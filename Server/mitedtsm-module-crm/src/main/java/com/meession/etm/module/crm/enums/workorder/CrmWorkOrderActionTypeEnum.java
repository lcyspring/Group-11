package com.meession.etm.module.crm.enums.workorder;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmWorkOrderActionTypeEnum {
    CREATE(1, "创建"), UPDATE(2, "修改"), START(3, "开始处理"), RETURN(4, "退回"),
    RESUBMIT(5, "重新提交"), COMPLETE(6, "完结"), ASSIGN(7, "分派"),
    CLAIM(8, "领取"), CC_UPDATE(9, "更新抄送人"), CHECK_IN(10, "移动签到"),
    SLA_PAUSE(11, "暂停 SLA"), SLA_RESUME(12, "恢复 SLA"), SLA_ESCALATE(13, "SLA 自动升级");

    private final Integer type;
    private final String name;
}

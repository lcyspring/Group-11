package com.meession.etm.module.crm.util;

import cn.hutool.core.lang.Assert;
import com.meession.etm.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;

/**
 * CRM 流程工具类
 *
 * @author HUIHUI
 */
public class CrmAuditStatusUtils {

    /**
     * BPM 审批结果转换
     *
     * @param bpmResult BPM 审批结果
     */
    public static Integer convertBpmResultToAuditStatus(Integer bpmResult) {
        Integer auditStatus = BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(bpmResult)
                ? CrmAuditStatusEnum.APPROVE.getStatus()
                : BpmProcessInstanceStatusEnum.REJECT.getStatus().equals(bpmResult)
                ? CrmAuditStatusEnum.REJECT.getStatus()
                : BpmProcessInstanceStatusEnum.CANCEL.getStatus().equals(bpmResult)
                ? CrmAuditStatusEnum.CANCEL.getStatus() : null;
        Assert.notNull(auditStatus, "BPM 审批结果({}) 转换失败", bpmResult);
        return auditStatus;
    }

}

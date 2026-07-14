package com.meession.etm.framework.common.biz.bpm.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * BPM 流程实例创建通用请求 DTO
 * 
 * 用于业务模块提交审批申请时的参数封装
 * 
 * @author jxq
 */
@Data
@Accessors(chain = true)
public class BpmProcessInstanceCreateCommonReqDTO {

    /**
     * 流程定义 Key
     * 
     * 例如：order-audit、contract-audit、leave-audit
     */
    @NotEmpty(message = "流程定义 Key 不能为空")
    private String processDefinitionKey;

    /**
     * 业务 Key（业务实体的唯一标识）
     * 
     * 例如：订单 ID、合同 ID、请假单 ID
     */
    @NotEmpty(message = "业务 Key 不能为空")
    private String businessKey;

    /**
     * 流程变量
     * 
     * 用于传递业务数据到流程引擎，例如：
     * - 请假天数（用于条件判断）
     * - 订单金额（用于审批分支）
     * - 其他业务字段
     */
    private Map<String, Object> variables;

    /**
     * 发起人自选审批人映射
     * 
     * Key: 任务节点 Key（例如：manager-approve、director-approve）
     * Value: 该节点的审批人用户 ID 列表
     * 
     * 用于动态指定审批人，例如：
     * - 部门经理审批：{"manager-approve": [1001, 1002]}
     * - 总监审批：{"director-approve": [2001]}
     */
    private Map<String, List<Long>> startUserSelectAssignees;

}

package com.meession.etm.module.bpm.framework.audit;

import cn.hutool.core.util.ObjUtil;
import com.meession.etm.framework.common.biz.bpm.enums.BpmAuditStatusEnum;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * BPM 审批集成抽象服务
 * 
 * 提供业务模块与 BPM 集成的通用逻辑，业务模块继承此类即可快速接入审批流程
 * 
 * 使用方式：
 * 1. 继承此类并实现抽象方法
 * 2. 定义流程定义 Key（BPM_PROCESS_DEFINITION_KEY）
 * 3. 实现业务实体的验证和状态更新逻辑
 * 
 * @author jxq
 */
@Slf4j
public abstract class AbstractBpmAuditService<T> {

    @Resource
    protected BpmProcessInstanceApi bpmProcessInstanceApi;

    /**
     * 获取流程定义 Key
     * 
     * @return 流程定义 Key（例如：order-audit、contract-audit）
     */
    protected abstract String getProcessDefinitionKey();

    /**
     * 验证业务实体是否存在且状态正确
     * 
     * @param id 业务实体 ID
     * @return 业务实体对象
     */
    protected abstract T validateEntityExists(Long id);

    /**
     * 验证业务实体是否可以提交审批
     * 
     * @param entity 业务实体
     */
    protected abstract void validateEntityCanSubmit(T entity);

    /**
     * 更新业务实体的流程实例 ID 和审批状态
     * 
     * @param id 业务实体 ID
     * @param processInstanceId 流程实例 ID
     * @param auditStatus 审批状态
     */
    protected abstract void updateEntityProcessInfo(Long id, String processInstanceId, Integer auditStatus);

    /**
     * 更新业务实体的审批状态
     * 
     * @param id 业务实体 ID
     * @param auditStatus 审批状态
     */
    protected abstract void updateEntityAuditStatus(Long id, Integer auditStatus);

    /**
     * 提交审批
     * 
     * @param id 业务实体 ID
     * @param userId 发起人用户 ID
     * @return 流程实例 ID
     */
    public String submitForApproval(Long id, Long userId) {
        // 1. 验证业务实体
        T entity = validateEntityExists(id);
        validateEntityCanSubmit(entity);

        // 2. 创建流程实例
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(getProcessDefinitionKey())
                        .setBusinessKey(String.valueOf(id)));

        // 3. 更新业务实体状态
        updateEntityProcessInfo(id, processInstanceId, BpmAuditStatusEnum.PROCESS.getStatus());

        log.info("[submitForApproval][业务实体({})提交审批，流程实例ID({})]", id, processInstanceId);
        return processInstanceId;
    }

    /**
     * 提交审批（带流程变量）
     * 
     * @param id 业务实体 ID
     * @param userId 发起人用户 ID
     * @param variables 流程变量
     * @return 流程实例 ID
     */
    public String submitForApproval(Long id, Long userId, java.util.Map<String, Object> variables) {
        // 1. 验证业务实体
        T entity = validateEntityExists(id);
        validateEntityCanSubmit(entity);

        // 2. 创建流程实例
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(getProcessDefinitionKey())
                        .setBusinessKey(String.valueOf(id))
                        .setVariables(variables));

        // 3. 更新业务实体状态
        updateEntityProcessInfo(id, processInstanceId, BpmAuditStatusEnum.PROCESS.getStatus());

        log.info("[submitForApproval][业务实体({})提交审批，流程实例ID({})]", id, processInstanceId);
        return processInstanceId;
    }

    /**
     * 提交审批（带流程变量和自选审批人）
     * 
     * @param id 业务实体 ID
     * @param userId 发起人用户 ID
     * @param variables 流程变量
     * @param startUserSelectAssignees 发起人自选审批人
     * @return 流程实例 ID
     */
    public String submitForApproval(Long id, Long userId, 
                                     java.util.Map<String, Object> variables,
                                     java.util.Map<String, java.util.List<Long>> startUserSelectAssignees) {
        // 1. 验证业务实体
        T entity = validateEntityExists(id);
        validateEntityCanSubmit(entity);

        // 2. 创建流程实例
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(getProcessDefinitionKey())
                        .setBusinessKey(String.valueOf(id))
                        .setVariables(variables)
                        .setStartUserSelectAssignees(startUserSelectAssignees));

        // 3. 更新业务实体状态
        updateEntityProcessInfo(id, processInstanceId, BpmAuditStatusEnum.PROCESS.getStatus());

        log.info("[submitForApproval][业务实体({})提交审批，流程实例ID({})]", id, processInstanceId);
        return processInstanceId;
    }

    /**
     * 处理审批结果
     * 
     * @param id 业务实体 ID
     * @param bpmResult BPM 审批结果
     */
    public void handleApprovalResult(Long id, Integer bpmResult) {
        // 1. 验证业务实体
        T entity = validateEntityExists(id);
        
        // 2. 验证是否处于审批中状态
        if (!isInProcessStatus(entity)) {
            log.error("[handleApprovalResult][业务实体({})不处于审批中，无法更新审批结果({})]", 
                    id, bpmResult);
            throw new RuntimeException("业务实体不处于审批中状态");
        }

        // 3. 转换 BPM 结果为业务审批状态
        Integer auditStatus = convertBpmResultToAuditStatus(bpmResult);

        // 4. 更新业务实体审批状态
        updateEntityAuditStatus(id, auditStatus);

        log.info("[handleApprovalResult][业务实体({})审批结果更新为({})]", id, auditStatus);
    }

    /**
     * 取消审批
     *
     * @param id 业务实体 ID
     * @param reason 取消原因
     */
    public void cancelApproval(Long id, String reason) {
        T entity = validateEntityExists(id);
        if (!isInProcessStatus(entity)) {
            throw new RuntimeException("业务实体不处于审批中状态");
        }
        // 获取流程实例ID并取消
        String processInstanceId = getProcessInstanceId(entity);
        if (processInstanceId != null) {
            bpmProcessInstanceApi.cancelProcessInstance(processInstanceId, reason);
        }
        updateEntityAuditStatus(id, BpmAuditStatusEnum.CANCEL.getStatus());
    }

    /**
     * 获取业务实体的流程实例ID
     *
     * @param entity 业务实体
     * @return 流程实例 ID
     */
    protected abstract String getProcessInstanceId(T entity);

    /**
     * 判断业务实体是否处于审批中状态
     *
     * @param entity 业务实体
     * @return 是否处于审批中
     */
    protected abstract boolean isInProcessStatus(T entity);

    /**
     * 将 BPM 审批结果转换为业务审批状态
     * 
     * @param bpmResult BPM 审批结果
     * @return 业务审批状态
     */
    protected Integer convertBpmResultToAuditStatus(Integer bpmResult) {
        // 这里需要根据 BpmTaskStatusEnum 进行转换
        // 由于 BpmTaskStatusEnum 在 BPM 模块内部，这里使用通用逻辑
        if (ObjUtil.equals(bpmResult, 2)) { // APPROVE
            return BpmAuditStatusEnum.APPROVE.getStatus();
        } else if (ObjUtil.equals(bpmResult, 3)) { // REJECT
            return BpmAuditStatusEnum.REJECT.getStatus();
        } else if (ObjUtil.equals(bpmResult, 4)) { // CANCEL
            return BpmAuditStatusEnum.CANCEL.getStatus();
        }
        throw new RuntimeException("BPM 审批结果(" + bpmResult + ") 转换失败");
    }

}

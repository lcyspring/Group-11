package com.meession.etm.module.bpm.framework.audit;

import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import lombok.extern.slf4j.Slf4j;

/**
 * BPM 审批状态监听器抽象基类
 * 
 * 业务模块继承此类，实现审批结果的自动处理
 * 
 * 使用方式：
 * 1. 继承此类
 * 2. 实现 getProcessDefinitionKey() 返回对应的流程定义 Key
 * 3. 实现 handleApprovalResult() 处理审批结果
 * 
 * @author jxq
 */
@Slf4j
public abstract class AbstractBpmStatusListener extends BpmProcessInstanceStatusEventListener {

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        log.info("[onEvent][收到审批结果事件：processDefinitionKey={}, businessKey={}, status={}]", 
                event.getProcessDefinitionKey(), event.getBusinessKey(), event.getStatus());

        try {
            // 解析业务 ID
            Long businessId = Long.parseLong(event.getBusinessKey());
            
            // 处理审批结果
            handleApprovalResult(businessId, event.getStatus(), event.getReason());
            
            log.info("[onEvent][审批结果处理成功：businessId={}, status={}]", businessId, event.getStatus());
        } catch (Exception e) {
            log.error("[onEvent][审批结果处理失败：businessKey={}, error={}]", 
                    event.getBusinessKey(), e.getMessage(), e);
            throw new RuntimeException("审批结果处理失败", e);
        }
    }

    /**
     * 处理审批结果
     * 
     * @param businessId 业务实体 ID
     * @param bpmStatus BPM 审批状态
     * @param reason 审批原因（拒绝或取消时的原因）
     */
    protected abstract void handleApprovalResult(Long businessId, Integer bpmStatus, String reason);

}

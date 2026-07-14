package com.meession.etm.module.bpm.api.common;

import com.meession.etm.framework.common.biz.bpm.BpmProcessInstanceCommonApi;
import com.meession.etm.framework.common.biz.bpm.dto.BpmProcessInstanceCreateCommonReqDTO;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * BPM 流程实例 Common API 实现类
 * 
 * 作为通用 API 的实现，将通用 DTO 转换为 BPM 模块内部 DTO
 * 
 * @author jxq
 */
@Service
@Validated
@Slf4j
public class BpmProcessInstanceCommonApiImpl implements BpmProcessInstanceCommonApi {

    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;

    @Override
    public String createProcessInstance(Long userId, BpmProcessInstanceCreateCommonReqDTO reqDTO) {
        log.info("[createProcessInstance][userId={}, processDefinitionKey={}, businessKey={}]", 
                userId, reqDTO.getProcessDefinitionKey(), reqDTO.getBusinessKey());

        // 将通用 DTO 转换为 BPM 内部 DTO
        BpmProcessInstanceCreateReqDTO bpmReqDTO = new BpmProcessInstanceCreateReqDTO();
        bpmReqDTO.setProcessDefinitionKey(reqDTO.getProcessDefinitionKey());
        bpmReqDTO.setBusinessKey(reqDTO.getBusinessKey());
        bpmReqDTO.setVariables(reqDTO.getVariables());
        bpmReqDTO.setStartUserSelectAssignees(reqDTO.getStartUserSelectAssignees());

        // 调用 BPM 模块内部 API
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId, bpmReqDTO);
        
        log.info("[createProcessInstance][创建成功，processInstanceId={}]", processInstanceId);
        return processInstanceId;
    }

}

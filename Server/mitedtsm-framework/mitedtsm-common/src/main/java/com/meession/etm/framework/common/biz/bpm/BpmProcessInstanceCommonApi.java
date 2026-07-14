package com.meession.etm.framework.common.biz.bpm;

import com.meession.etm.framework.common.biz.bpm.dto.BpmProcessInstanceCreateCommonReqDTO;
import jakarta.validation.Valid;

/**
 * BPM 流程实例 Common API 接口
 * 
 * 用于业务模块与 BPM 模块的集成，提供统一的流程实例创建接口
 * 
 * @author jxq
 */
public interface BpmProcessInstanceCommonApi {

    /**
     * 创建流程实例
     * 
     * @param userId 发起人用户 ID
     * @param reqDTO 创建请求 DTO
     * @return 流程实例 ID
     */
    String createProcessInstance(Long userId, @Valid BpmProcessInstanceCreateCommonReqDTO reqDTO);

}

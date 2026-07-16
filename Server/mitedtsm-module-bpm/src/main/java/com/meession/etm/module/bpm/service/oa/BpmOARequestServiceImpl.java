package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOARequestCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOARequestPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOARequestDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOARequestMapper;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_REQUEST_NOT_EXISTS;

@Service
@Validated
public class BpmOARequestServiceImpl implements BpmOARequestService {

    public static final String PROCESS_KEY = "oa_request";

    @Resource
    private BpmOARequestMapper requestMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRequest(Long userId, BpmOARequestCreateReqVO createReqVO) {
        BpmOARequestDO request = BeanUtils.toBean(createReqVO, BpmOARequestDO.class)
                .setUserId(userId).setStatus(BpmTaskStatusEnum.RUNNING.getStatus());
        requestMapper.insert(request);

        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("title", createReqVO.getTitle());
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(request.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));

        requestMapper.updateById(new BpmOARequestDO().setId(request.getId()).setProcessInstanceId(processInstanceId));
        return request.getId();
    }

    @Override
    public void updateRequestStatus(Long id, Integer status) {
        validateRequestExists(id);
        requestMapper.updateById(new BpmOARequestDO().setId(id).setStatus(status));
    }

    private void validateRequestExists(Long id) {
        if (requestMapper.selectById(id) == null) {
            throw exception(OA_REQUEST_NOT_EXISTS);
        }
    }

    @Override
    public BpmOARequestDO getRequest(Long id) {
        return requestMapper.selectById(id);
    }

    @Override
    public PageResult<BpmOARequestDO> getRequestPage(Long userId, BpmOARequestPageReqVO pageReqVO) {
        return requestMapper.selectPage(userId, pageReqVO);
    }

}
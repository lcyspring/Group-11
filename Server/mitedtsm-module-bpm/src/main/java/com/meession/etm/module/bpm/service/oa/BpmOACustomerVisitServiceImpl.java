package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOACustomerVisitCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOACustomerVisitPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOACustomerVisitDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOACustomerVisitMapper;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_CUSTOMER_VISIT_NOT_EXISTS;

@Service
@Validated
public class BpmOACustomerVisitServiceImpl implements BpmOACustomerVisitService {

    public static final String PROCESS_KEY = "oa_customer_visit";

    @Resource
    private BpmOACustomerVisitMapper customerVisitMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCustomerVisit(Long userId, BpmOACustomerVisitCreateReqVO createReqVO) {
        BpmOACustomerVisitDO visit = BeanUtils.toBean(createReqVO, BpmOACustomerVisitDO.class)
                .setUserId(userId).setStatus(BpmTaskStatusEnum.RUNNING.getStatus());
        customerVisitMapper.insert(visit);

        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("customerName", createReqVO.getCustomerName());
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(visit.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));

        customerVisitMapper.updateById(new BpmOACustomerVisitDO().setId(visit.getId()).setProcessInstanceId(processInstanceId));
        return visit.getId();
    }

    @Override
    public void updateCustomerVisitStatus(Long id, Integer status) {
        validateCustomerVisitExists(id);
        customerVisitMapper.updateById(new BpmOACustomerVisitDO().setId(id).setStatus(status));
    }

    private void validateCustomerVisitExists(Long id) {
        if (customerVisitMapper.selectById(id) == null) {
            throw exception(OA_CUSTOMER_VISIT_NOT_EXISTS);
        }
    }

    @Override
    public BpmOACustomerVisitDO getCustomerVisit(Long id) {
        return customerVisitMapper.selectById(id);
    }

    @Override
    public PageResult<BpmOACustomerVisitDO> getCustomerVisitPage(Long userId, BpmOACustomerVisitPageReqVO pageReqVO) {
        return customerVisitMapper.selectPage(userId, pageReqVO);
    }

}
package com.meession.etm.module.bpm.service.oa;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABusinessTripCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABusinessTripPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOABusinessTripDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOABusinessTripMapper;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_BUSINESS_TRIP_NOT_EXISTS;

@Service
@Validated
public class BpmOABusinessTripServiceImpl implements BpmOABusinessTripService {

    public static final String PROCESS_KEY = "oa_business_trip";

    @Resource
    private BpmOABusinessTripMapper businessTripMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBusinessTrip(Long userId, BpmOABusinessTripCreateReqVO createReqVO) {
        long day = LocalDateTimeUtil.between(createReqVO.getStartTime(), createReqVO.getEndTime()).toDays();
        BpmOABusinessTripDO businessTrip = BeanUtils.toBean(createReqVO, BpmOABusinessTripDO.class)
                .setUserId(userId).setDay(day).setStatus(BpmTaskStatusEnum.RUNNING.getStatus());
        businessTripMapper.insert(businessTrip);

        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("day", day);
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(businessTrip.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));

        businessTripMapper.updateById(new BpmOABusinessTripDO().setId(businessTrip.getId()).setProcessInstanceId(processInstanceId));
        return businessTrip.getId();
    }

    @Override
    public void updateBusinessTripStatus(Long id, Integer status) {
        validateBusinessTripExists(id);
        businessTripMapper.updateById(new BpmOABusinessTripDO().setId(id).setStatus(status));
    }

    private void validateBusinessTripExists(Long id) {
        if (businessTripMapper.selectById(id) == null) {
            throw exception(OA_BUSINESS_TRIP_NOT_EXISTS);
        }
    }

    @Override
    public BpmOABusinessTripDO getBusinessTrip(Long id) {
        return businessTripMapper.selectById(id);
    }

    @Override
    public PageResult<BpmOABusinessTripDO> getBusinessTripPage(Long userId, BpmOABusinessTripPageReqVO pageReqVO) {
        return businessTripMapper.selectPage(userId, pageReqVO);
    }

}
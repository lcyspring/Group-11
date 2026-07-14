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

/**
 * 出差申请 Service 实现类
 *
 * @author 李春雨
 */
@Service
@Validated
public class BpmOABusinessTripServiceImpl implements BpmOABusinessTripService {

    /**
     * 流程定义Key，对应Flowable流程定义
     */
    public static final String PROCESS_KEY = "oa_business_trip";

    @Resource
    private BpmOABusinessTripMapper businessTripMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    /**
     * 创建出差申请并启动审批流程
     *
     * @param userId        申请人ID
     * @param createReqVO   出差申请创建参数
     * @return 出差申请ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBusinessTrip(Long userId, BpmOABusinessTripCreateReqVO createReqVO) {
        // 计算出差天数：结束时间 - 开始时间
        long day = LocalDateTimeUtil.between(createReqVO.getStartTime(), createReqVO.getEndTime()).toDays();
        // 构建出差申请数据对象，设置用户ID、出差天数和初始状态
        BpmOABusinessTripDO businessTrip = BeanUtils.toBean(createReqVO, BpmOABusinessTripDO.class)
                .setUserId(userId).setDays(day).setStatus(BpmTaskStatusEnum.RUNNING.getStatus());
        // 保存出差申请记录
        businessTripMapper.insert(businessTrip);

        // 构建流程实例变量，传递出差天数给审批流程
        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("day", day);
        // 创建并启动审批流程实例
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(businessTrip.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));

        // 更新出差申请记录，关联流程实例ID
        businessTripMapper.updateById(new BpmOABusinessTripDO().setId(businessTrip.getId()).setProcessInstanceId(processInstanceId));
        return businessTrip.getId();
    }

    /**
     * 更新出差申请状态
     *
     * @param id     出差申请ID
     * @param status 审批状态
     */
    @Override
    public void updateBusinessTripStatus(Long id, Integer status) {
        // 校验出差申请是否存在
        validateBusinessTripExists(id);
        // 更新状态
        businessTripMapper.updateById(new BpmOABusinessTripDO().setId(id).setStatus(status));
    }

    /**
     * 校验出差申请是否存在，不存在则抛出异常
     *
     * @param id 出差申请ID
     */
    private void validateBusinessTripExists(Long id) {
        if (businessTripMapper.selectById(id) == null) {
            throw exception(OA_BUSINESS_TRIP_NOT_EXISTS);
        }
    }

    /**
     * 根据ID获取出差申请详情
     *
     * @param id 出差申请ID
     * @return 出差申请数据对象
     */
    @Override
    public BpmOABusinessTripDO getBusinessTrip(Long id) {
        return businessTripMapper.selectById(id);
    }

    /**
     * 分页查询出差申请列表
     *
     * @param userId     用户ID（可选，用于筛选当前用户的申请）
     * @param pageReqVO 分页查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<BpmOABusinessTripDO> getBusinessTripPage(Long userId, BpmOABusinessTripPageReqVO pageReqVO) {
        return businessTripMapper.selectPage(userId, pageReqVO);
    }

}
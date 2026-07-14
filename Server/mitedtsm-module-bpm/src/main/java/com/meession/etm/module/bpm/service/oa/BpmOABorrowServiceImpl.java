package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABorrowCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABorrowPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOABorrowDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOABorrowMapper;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_BORROW_NOT_EXISTS;

/**
 * 借款申请 Service 实现类
 *
 * @author 李春雨
 */
@Service
@Validated
public class BpmOABorrowServiceImpl implements BpmOABorrowService {

    /**
     * 流程定义Key，对应Flowable流程定义
     */
    public static final String PROCESS_KEY = "oa_borrow";

    @Resource
    private BpmOABorrowMapper borrowMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    /**
     * 创建借款申请并启动审批流程
     *
     * @param userId      申请人ID
     * @param createReqVO 借款申请创建参数
     * @return 借款申请ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBorrow(Long userId, BpmOABorrowCreateReqVO createReqVO) {
        // 构建借款申请数据对象，设置用户ID和初始状态
        BpmOABorrowDO borrow = BeanUtils.toBean(createReqVO, BpmOABorrowDO.class)
                .setUserId(userId).setStatus(BpmTaskStatusEnum.RUNNING.getStatus());
        // 保存借款申请记录
        borrowMapper.insert(borrow);

        // 构建流程实例变量，传递借款金额给审批流程
        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("amount", createReqVO.getAmount());
        // 创建并启动审批流程实例
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(borrow.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));

        // 更新借款申请记录，关联流程实例ID
        borrowMapper.updateById(new BpmOABorrowDO().setId(borrow.getId()).setProcessInstanceId(processInstanceId));
        return borrow.getId();
    }

    /**
     * 更新借款申请状态
     *
     * @param id     借款申请ID
     * @param status 审批状态
     */
    @Override
    public void updateBorrowStatus(Long id, Integer status) {
        // 校验借款申请是否存在
        validateBorrowExists(id);
        // 更新状态
        borrowMapper.updateById(new BpmOABorrowDO().setId(id).setStatus(status));
    }

    /**
     * 校验借款申请是否存在，不存在则抛出异常
     *
     * @param id 借款申请ID
     */
    private void validateBorrowExists(Long id) {
        if (borrowMapper.selectById(id) == null) {
            throw exception(OA_BORROW_NOT_EXISTS);
        }
    }

    /**
     * 根据ID获取借款申请详情
     *
     * @param id 借款申请ID
     * @return 借款申请数据对象
     */
    @Override
    public BpmOABorrowDO getBorrow(Long id) {
        return borrowMapper.selectById(id);
    }

    /**
     * 分页查询借款申请列表
     *
     * @param userId    用户ID（可选，用于筛选当前用户的申请）
     * @param pageReqVO 分页查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<BpmOABorrowDO> getBorrowPage(Long userId, BpmOABorrowPageReqVO pageReqVO) {
        return borrowMapper.selectPage(userId, pageReqVO);
    }

}
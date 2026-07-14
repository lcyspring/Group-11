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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_BORROW_NOT_EXISTS;

@Service
@Validated
public class BpmOABorrowServiceImpl implements BpmOABorrowService {

    public static final String PROCESS_KEY = "oa_borrow";

    @Resource
    private BpmOABorrowMapper borrowMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBorrow(Long userId, BpmOABorrowCreateReqVO createReqVO) {
        BpmOABorrowDO borrow = BeanUtils.toBean(createReqVO, BpmOABorrowDO.class)
                .setUserId(userId).setStatus(BpmTaskStatusEnum.RUNNING.getStatus());
        borrowMapper.insert(borrow);

        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("amount", createReqVO.getAmount());
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(borrow.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));

        borrowMapper.updateById(new BpmOABorrowDO().setId(borrow.getId()).setProcessInstanceId(processInstanceId));
        return borrow.getId();
    }

    @Override
    public void updateBorrowStatus(Long id, Integer status) {
        validateBorrowExists(id);
        borrowMapper.updateById(new BpmOABorrowDO().setId(id).setStatus(status));
    }

    private void validateBorrowExists(Long id) {
        if (borrowMapper.selectById(id) == null) {
            throw exception(OA_BORROW_NOT_EXISTS);
        }
    }

    @Override
    public BpmOABorrowDO getBorrow(Long id) {
        return borrowMapper.selectById(id);
    }

    @Override
    public PageResult<BpmOABorrowDO> getBorrowPage(Long userId, BpmOABorrowPageReqVO pageReqVO) {
        return borrowMapper.selectPage(userId, pageReqVO);
    }

}
package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAWorkRequestCreateReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOAWorkRequestDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOAWorkRequestMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_TASK_NOT_EXISTS;

@Service
public class BpmOAWorkRequestServiceImpl implements BpmOAWorkRequestService {
    public static final String PROCESS_KEY = "oa_work_request";
    @Resource private BpmOAWorkRequestMapper mapper;
    @Resource private BpmProcessInstanceApi processInstanceApi;
    @Override @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, BpmOAWorkRequestCreateReqVO req) {
        BpmOAWorkRequestDO row = BeanUtils.toBean(req, BpmOAWorkRequestDO.class).setUserId(userId).setStatus(1);
        mapper.insert(row);
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", row.getTitle());
        variables.put("urgency", row.getUrgency());
        String processId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setBusinessKey(String.valueOf(row.getId()))
                        .setVariables(variables)
                        .setStartUserSelectAssignees(req.getStartUserSelectAssignees()));
        mapper.updateById(new BpmOAWorkRequestDO().setId(row.getId()).setProcessInstanceId(processId));
        return row.getId();
    }
    @Override public BpmOAWorkRequestDO get(Long userId, Long id) {
        BpmOAWorkRequestDO row = mapper.selectById(id);
        if (row == null) throw exception(OA_TASK_NOT_EXISTS);
        return row;
    }
    @Override public List<BpmOAWorkRequestDO> list(Long userId) { return mapper.selectByUserId(userId); }
    @Override public void updateStatus(Long id, Integer status) {
        BpmOAWorkRequestDO update = new BpmOAWorkRequestDO().setId(id).setStatus(status);
        if (Objects.equals(status, 2)) {
            update.setApprovedTime(LocalDateTime.now());
        }
        mapper.updateById(update);
    }
}

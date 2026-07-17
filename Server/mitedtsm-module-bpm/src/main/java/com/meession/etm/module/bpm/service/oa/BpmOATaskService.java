package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOATaskSaveReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOATaskDO;
import java.util.List;

public interface BpmOATaskService {
    Long create(Long userId, BpmOATaskSaveReqVO req);
    void update(Long userId, BpmOATaskSaveReqVO req);
    void delete(Long userId, Long id);
    void start(Long userId, Long id);
    void complete(Long userId, Long id, String result);
    BpmOATaskDO get(Long userId, Long id);
    List<BpmOATaskDO> list(Long userId, Integer status);
    int remindDue(int limit);
}

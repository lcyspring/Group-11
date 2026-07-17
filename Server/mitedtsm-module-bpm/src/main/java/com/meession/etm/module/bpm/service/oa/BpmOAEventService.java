package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAEventSaveReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOAEventDO;
import java.time.LocalDateTime;
import java.util.List;

public interface BpmOAEventService {
    Long create(Long userId, BpmOAEventSaveReqVO req);
    void update(Long userId, BpmOAEventSaveReqVO req);
    void delete(Long userId, Long id);
    BpmOAEventDO get(Long userId, Long id);
    List<BpmOAEventDO> list(Long userId, LocalDateTime from, LocalDateTime to);
}

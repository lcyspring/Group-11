package com.meession.etm.module.bpm.service.oa;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAWorkRequestCreateReqVO; import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOAWorkRequestDO; import java.util.List;
public interface BpmOAWorkRequestService { Long create(Long userId, BpmOAWorkRequestCreateReqVO req); BpmOAWorkRequestDO get(Long userId, Long id); List<BpmOAWorkRequestDO> list(Long userId); void updateStatus(Long id,Integer status); }

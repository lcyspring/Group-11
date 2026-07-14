package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmPerformanceTargetBaseReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmPerformanceTargetListReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmPerformanceTargetSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.statistics.CrmPerformanceTargetDO;

import java.util.List;

public interface CrmPerformanceTargetService {

    void savePerformanceTarget(CrmPerformanceTargetSaveReqVO reqVO);

    void deletePerformanceTarget(CrmPerformanceTargetBaseReqVO reqVO);

    List<CrmPerformanceTargetDO> getPerformanceTargetList(CrmPerformanceTargetListReqVO reqVO);

}

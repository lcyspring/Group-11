package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.win.CrmStatisticsWinAnalysisReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.win.CrmStatisticsWinAnalysisRespVO;

public interface CrmStatisticsWinAnalysisService {

    CrmStatisticsWinAnalysisRespVO getWinAnalysis(CrmStatisticsWinAnalysisReqVO reqVO);

}
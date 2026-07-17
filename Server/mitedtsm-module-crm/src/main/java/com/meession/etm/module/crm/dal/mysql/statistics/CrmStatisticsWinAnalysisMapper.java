package com.meession.etm.module.crm.dal.mysql.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.win.CrmStatisticsWinAnalysisReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.win.CrmStatisticsWinAnalysisRespVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmStatisticsWinAnalysisMapper {

    Map<String, Object> selectBusinessWinSummary(CrmStatisticsWinAnalysisReqVO reqVO);

    List<CrmStatisticsWinAnalysisRespVO.WinRateByDateItem> selectWinRateByDate(CrmStatisticsWinAnalysisReqVO reqVO);

    List<CrmStatisticsWinAnalysisRespVO.WinAmountByIndustryItem> selectWinAmountByIndustry(CrmStatisticsWinAnalysisReqVO reqVO);

    List<CrmStatisticsWinAnalysisRespVO.WinAmountByCustomerLevelItem> selectWinAmountByCustomerLevel(CrmStatisticsWinAnalysisReqVO reqVO);

}
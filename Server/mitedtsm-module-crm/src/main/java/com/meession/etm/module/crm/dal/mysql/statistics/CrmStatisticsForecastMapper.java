package com.meession.etm.module.crm.dal.mysql.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.forecast.CrmStatisticsForecastReqVO;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface CrmStatisticsForecastMapper {

    List<Map<String, Object>> selectWinBusinessByDate(CrmStatisticsForecastReqVO reqVO);

    List<Map<String, Object>> selectBusinessCountByDate(CrmStatisticsForecastReqVO reqVO);

    BigDecimal selectTotalWinAmount(CrmStatisticsForecastReqVO reqVO);

    Long selectWinBusinessCount(CrmStatisticsForecastReqVO reqVO);

    BigDecimal selectTotalBusinessAmount(CrmStatisticsForecastReqVO reqVO);

    Long selectBusinessCount(CrmStatisticsForecastReqVO reqVO);

}
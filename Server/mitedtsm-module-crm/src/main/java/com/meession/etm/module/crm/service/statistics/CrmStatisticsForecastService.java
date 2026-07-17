package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.forecast.CrmStatisticsForecastReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.forecast.CrmStatisticsForecastRespVO;

public interface CrmStatisticsForecastService {

    CrmStatisticsForecastRespVO getForecast(CrmStatisticsForecastReqVO reqVO);

}
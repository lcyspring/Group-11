package com.meession.etm.module.crm.controller.admin.statistics;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.crm.controller.admin.statistics.vo.forecast.CrmStatisticsForecastReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.forecast.CrmStatisticsForecastRespVO;
import com.meession.etm.module.crm.service.statistics.CrmStatisticsForecastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CRM 销售预测")
@RestController
@RequestMapping("/crm/statistics/forecast")
public class CrmStatisticsForecastController {

    @Resource
    private CrmStatisticsForecastService forecastService;

    @PostMapping("/get")
    @Operation(summary = "获取销售预测数据")
    public CommonResult<CrmStatisticsForecastRespVO> getForecast(@Valid @RequestBody CrmStatisticsForecastReqVO reqVO) {
        return success(forecastService.getForecast(reqVO));
    }

}
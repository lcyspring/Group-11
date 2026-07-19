package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.*;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;

import java.util.List;

/**
 * CRM 销售漏斗分析 Service
 *
 * @author HUIHUI
 */
public interface CrmStatisticsFunnelService {

    /**
     * 获得销售漏斗数据
     *
     * @param reqVO 请求
     * @return 销售漏斗数据
     */
    CrmStatisticFunnelSummaryRespVO getFunnelSummary(CrmStatisticsFunnelReqVO reqVO);

    /**
     * 获得按商机状态组计算的阶段漏斗。
     */
    List<CrmStatisticsBusinessStageSummaryRespVO> getBusinessStageSummary(CrmStatisticsBusinessStageReqVO reqVO);

    /**
     * 获得所选阶段及后续活跃商机、赢单商机明细。
     */
    PageResult<CrmBusinessDO> getBusinessStagePage(CrmStatisticsBusinessStagePageReqVO pageVO);

    /**
     * 获得赢单商机明细。
     */
    PageResult<CrmBusinessDO> getBusinessWonPage(CrmStatisticsBusinessStageReqVO pageVO);

    /** 获得赢单、输单或无效商机明细。 */
    PageResult<CrmBusinessDO> getBusinessOutcomePage(CrmStatisticsBusinessOutcomePageReqVO pageVO);

    /**
     * 获得商机结束状态统计
     *
     * @param reqVO 请求
     * @return 商机结束状态统计
     */
    List<CrmStatisticsBusinessSummaryByEndStatusRespVO> getBusinessSummaryByEndStatus(CrmStatisticsFunnelReqVO reqVO);

    /**
     * 获取新增商机分析(按日期)
     *
     * @param reqVO 请求
     * @return 新增商机分析
     */
    List<CrmStatisticsBusinessSummaryByDateRespVO> getBusinessSummaryByDate(CrmStatisticsFunnelReqVO reqVO);

    /**
     * 获得商机转化率分析(按日期)
     *
     * @param reqVO 请求
     * @return 商机转化率分析
     */
    List<CrmStatisticsBusinessInversionRateSummaryByDateRespVO> getBusinessInversionRateSummaryByDate(CrmStatisticsFunnelReqVO reqVO);

    /**
     * 获得销售预测汇总。
     */
    List<CrmStatisticsBusinessForecastByDateRespVO> getBusinessForecastByDate(CrmStatisticsFunnelReqVO reqVO);

    /**
     * 获得商机分页(按日期)
     *
     * @param pageVO 请求
     * @return 商机分页
     */
    PageResult<CrmBusinessDO> getBusinessPageByDate(CrmStatisticsFunnelReqVO pageVO);

    /**
     * 获得销售预测商机分页。
     */
    PageResult<CrmBusinessDO> getBusinessForecastPage(CrmStatisticsFunnelReqVO pageVO);

}

package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.statistics.vo.portrait.*;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;

import java.util.List;

/**
 * CRM 客户画像 Service 接口
 *
 * @author HUIHUI
 */
public interface CrmStatisticsPortraitService {

    /**
     * 获取客户省份统计数据
     *
     * @param reqVO 请求参数
     * @return 统计数据
     */
    List<CrmStatisticCustomerAreaRespVO> getCustomerSummaryByArea(CrmStatisticsPortraitReqVO reqVO);

    /**
     * 获取客户城市统计数据。
     */
    List<CrmStatisticCustomerAreaRespVO> getCustomerSummaryByCity(CrmStatisticsPortraitReqVO reqVO);

    /**
     * 获取客户国家统计数据。
     */
    List<CrmStatisticCustomerAreaRespVO> getCustomerSummaryByCountry(CrmStatisticsPortraitReqVO reqVO);

    /**
     * 获取指定城市、省份或国家下的客户明细分页。
     */
    PageResult<CrmCustomerDO> getCustomerPageByArea(CrmStatisticsPortraitCustomerPageReqVO reqVO);

    /**
     * 获取客户行业统计数据
     *
     * @param reqVO 请求参数
     * @return 统计数据
     */
    List<CrmStatisticCustomerIndustryRespVO> getCustomerSummaryByIndustry(CrmStatisticsPortraitReqVO reqVO);

    /**
     * 获取客户级别统计数据
     *
     * @param reqVO 请求参数
     * @return 统计数据
     */
    List<CrmStatisticCustomerLevelRespVO> getCustomerSummaryByLevel(CrmStatisticsPortraitReqVO reqVO);

    /**
     * 获取客户来源统计数据
     *
     * @param reqVO 请求参数
     * @return 统计数据
     */
    List<CrmStatisticCustomerSourceRespVO> getCustomerSummaryBySource(CrmStatisticsPortraitReqVO reqVO);

    /**
     * 获取客户成交状态分布。
     */
    List<CrmStatisticCustomerDealStatusRespVO> getCustomerSummaryByDealStatus(CrmStatisticsPortraitReqVO reqVO);

}

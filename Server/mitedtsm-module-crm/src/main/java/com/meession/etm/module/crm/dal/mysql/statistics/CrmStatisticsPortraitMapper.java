package com.meession.etm.module.crm.dal.mysql.statistics;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.statistics.vo.portrait.*;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * CRM 数据画像 Mapper
 *
 * @author HUIHUI
 */
@Mapper
public interface CrmStatisticsPortraitMapper extends BaseMapperX<CrmCustomerDO> {

    List<CrmStatisticCustomerAreaRespVO> selectSummaryListGroupByAreaId(CrmStatisticsPortraitReqVO reqVO);

    List<CrmStatisticCustomerIndustryRespVO> selectCustomerIndustryListGroupByIndustryId(CrmStatisticsPortraitReqVO reqVO);

    List<CrmStatisticCustomerSourceRespVO> selectCustomerSourceListGroupBySource(CrmStatisticsPortraitReqVO reqVO);

    List<CrmStatisticCustomerLevelRespVO> selectCustomerLevelListGroupByLevel(CrmStatisticsPortraitReqVO reqVO);

    List<CrmStatisticCustomerDealStatusRespVO> selectCustomerDealStatusList(CrmStatisticsPortraitReqVO reqVO);

    default PageResult<CrmCustomerDO> selectCustomerPageByArea(CrmStatisticsPortraitCustomerPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CrmCustomerDO>()
                .in(CrmCustomerDO::getOwnerUserId, reqVO.getUserIds())
                .in(CrmCustomerDO::getAreaId, reqVO.getAreaIds())
                .betweenIfPresent(CrmCustomerDO::getCreateTime, reqVO.getTimes())
                .orderByDesc(CrmCustomerDO::getCreateTime)
                .orderByDesc(CrmCustomerDO::getId));
    }

}

package com.meession.etm.module.crm.dal.mysql.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.marketing.vo.sms.CrmMarketingSmsTemplatePageReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingSmsTemplateDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 营销短信模板 Mapper
 *
 * @author mitedtsm
 */
@Mapper
public interface CrmMarketingSmsTemplateMapper extends BaseMapperX<CrmMarketingSmsTemplateDO> {

    default PageResult<CrmMarketingSmsTemplateDO> selectPage(CrmMarketingSmsTemplatePageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<CrmMarketingSmsTemplateDO>()
                .likeIfPresent(CrmMarketingSmsTemplateDO::getName, pageReqVO.getName())
                .eqIfPresent(CrmMarketingSmsTemplateDO::getCode, pageReqVO.getCode())
                .eqIfPresent(CrmMarketingSmsTemplateDO::getStatus, pageReqVO.getStatus())
                .eqIfPresent(CrmMarketingSmsTemplateDO::getCampaignId, pageReqVO.getCampaignId())
                .betweenIfPresent(CrmMarketingSmsTemplateDO::getCreateTime, pageReqVO.getCreateTime())
                .orderByDesc(CrmMarketingSmsTemplateDO::getId));
    }

    default CrmMarketingSmsTemplateDO selectByCode(String code) {
        return selectOne(CrmMarketingSmsTemplateDO::getCode, code);
    }

}

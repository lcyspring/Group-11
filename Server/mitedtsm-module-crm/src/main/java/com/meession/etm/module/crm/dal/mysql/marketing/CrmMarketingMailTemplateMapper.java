package com.meession.etm.module.crm.dal.mysql.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.marketing.vo.mail.CrmMarketingMailTemplatePageReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingMailTemplateDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 营销邮件模板 Mapper
 *
 * @author mitedtsm
 */
@Mapper
public interface CrmMarketingMailTemplateMapper extends BaseMapperX<CrmMarketingMailTemplateDO> {

    default PageResult<CrmMarketingMailTemplateDO> selectPage(CrmMarketingMailTemplatePageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<CrmMarketingMailTemplateDO>()
                .likeIfPresent(CrmMarketingMailTemplateDO::getName, pageReqVO.getName())
                .eqIfPresent(CrmMarketingMailTemplateDO::getCode, pageReqVO.getCode())
                .eqIfPresent(CrmMarketingMailTemplateDO::getStatus, pageReqVO.getStatus())
                .eqIfPresent(CrmMarketingMailTemplateDO::getCampaignId, pageReqVO.getCampaignId())
                .betweenIfPresent(CrmMarketingMailTemplateDO::getCreateTime, pageReqVO.getCreateTime())
                .orderByDesc(CrmMarketingMailTemplateDO::getId));
    }

    default CrmMarketingMailTemplateDO selectByCode(String code) {
        return selectOne(CrmMarketingMailTemplateDO::getCode, code);
    }

}

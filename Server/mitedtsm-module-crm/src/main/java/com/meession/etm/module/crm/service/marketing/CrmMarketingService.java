package com.meession.etm.module.crm.service.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.marketing.vo.*;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmCompetitorDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingCampaignDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingCampaignRelationDO;

import java.util.List;

public interface CrmMarketingService {
    Long saveCampaign(CrmMarketingCampaignSaveReqVO request, Long userId);
    void startCampaign(Long id, Long userId);
    void lockCampaign(Long id, Long userId);
    void terminateCampaign(CrmMarketingCampaignActionReqVO request, Long userId);
    void completeCampaign(CrmMarketingCampaignActionReqVO request, Long userId);
    CrmMarketingCampaignDO getCampaign(Long id, Long userId);
    PageResult<CrmMarketingCampaignDO> getCampaignPage(CrmMarketingCampaignPageReqVO request, Long userId);
    List<CrmMarketingCampaignRelationDO> getCampaignRelations(Long id, Long userId);

    Long saveCompetitor(CrmCompetitorSaveReqVO request, Long userId);
    void deleteCompetitor(Long id, Long userId);
    PageResult<CrmCompetitorDO> getCompetitorPage(CrmCompetitorPageReqVO request, Long userId);
}

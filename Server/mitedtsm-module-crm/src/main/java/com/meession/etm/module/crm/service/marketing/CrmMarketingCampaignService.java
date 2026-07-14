package com.meession.etm.module.crm.service.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.marketing.vo.campaign.CrmMarketingCampaignPageReqVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.campaign.CrmMarketingCampaignSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingCampaignDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * 营销活动 Service 接口
 *
 * @author mitedtsm
 */
public interface CrmMarketingCampaignService {

    /**
     * 创建营销活动
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCampaign(@Valid CrmMarketingCampaignSaveReqVO createReqVO);

    /**
     * 更新营销活动
     *
     * @param updateReqVO 更新信息
     */
    void updateCampaign(@Valid CrmMarketingCampaignSaveReqVO updateReqVO);

    /**
     * 删除营销活动
     *
     * @param id 编号
     */
    void deleteCampaign(Long id);

    /**
     * 获得营销活动
     *
     * @param id 编号
     * @return 营销活动
     */
    CrmMarketingCampaignDO getCampaign(Long id);

    /**
     * 获得营销活动列表
     *
     * @param ids 编号
     * @return 营销活动列表
     */
    List<CrmMarketingCampaignDO> getCampaignList(Collection<Long> ids);

    /**
     * 获得营销活动 Map
     *
     * @param ids 编号
     * @return 营销活动 Map
     */
    default Map<Long, CrmMarketingCampaignDO> getCampaignMap(Collection<Long> ids) {
        return convertMap(getCampaignList(ids), CrmMarketingCampaignDO::getId);
    }

    /**
     * 获得营销活动分页
     *
     * @param pageReqVO 分页查询
     * @return 营销活动分页
     */
    PageResult<CrmMarketingCampaignDO> getCampaignPage(CrmMarketingCampaignPageReqVO pageReqVO);

}

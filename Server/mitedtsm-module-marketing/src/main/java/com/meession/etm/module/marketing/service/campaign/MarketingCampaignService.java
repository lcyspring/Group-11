package com.meession.etm.module.marketing.service.campaign;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.marketing.controller.admin.campaign.vo.CampaignPageReqVO;
import com.meession.etm.module.marketing.controller.admin.campaign.vo.CampaignSaveReqVO;
import com.meession.etm.module.marketing.dal.dataobject.campaign.MarketingCampaignDO;
import jakarta.validation.Valid;

/**
 * 营销活动 Service 接口
 *
 * @author MITEDTSM
 */
public interface MarketingCampaignService {

    /**
     * 创建营销活动
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCampaign(@Valid CampaignSaveReqVO createReqVO);

    /**
     * 更新营销活动
     *
     * @param updateReqVO 更新信息
     */
    void updateCampaign(@Valid CampaignSaveReqVO updateReqVO);

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
    MarketingCampaignDO getCampaign(Long id);

    /**
     * 获得营销活动分页
     *
     * @param pageReqVO 分页查询
     * @return 营销活动分页
     */
    PageResult<MarketingCampaignDO> getCampaignPage(CampaignPageReqVO pageReqVO);

    /**
     * 提交审核（创建 BPM 流程实例）
     *
     * @param id     活动编号
     * @param userId 用户编号
     * @return BPM 流程实例ID
     */
    String submitApproval(Long id, Long userId);

    /**
     * 启动营销活动（执行批量发送）
     *
     * @param id 活动编号
     */
    void startCampaign(Long id);

    /**
     * 取消营销活动
     *
     * @param id 活动编号
     */
    void cancelCampaign(Long id);

    /**
     * 更新活动发送统计
     *
     * @param id           活动编号
     * @param sentCount    新增已发送数量
     * @param successCount 新增成功数量
     * @param failCount    新增失败数量
     */
    void updateSendStatistics(Long id, Integer sentCount, Integer successCount, Integer failCount);

}

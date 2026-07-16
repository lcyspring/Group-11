package com.meession.etm.module.marketing.service.campaign;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.pojo.PageParam;
import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import com.meession.etm.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.meession.etm.module.marketing.dal.dataobject.campaign.MarketingCampaignDO;
import com.meession.etm.module.marketing.dal.mysql.campaign.MarketingCampaignMapper;
import com.meession.etm.module.marketing.enums.CampaignStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 营销活动审批结果监听器
 * <p>
 * 监听 BPM 流程实例状态变化，根据审批结果更新活动状态：
 * - 审批通过 → APPROVED
 * - 审批拒绝 → back to DRAFT
 * - 审批取消 → CANCELLED
 *
 * @author MITEDTSM
 */
@Slf4j
@Component
public class CampaignApprovalListener extends BpmProcessInstanceStatusEventListener {

    private static final String PROCESS_DEFINITION_KEY = "marketing-campaign-approval";

    @Resource
    private MarketingCampaignMapper campaignMapper;

    @Override
    protected String getProcessDefinitionKey() {
        return PROCESS_DEFINITION_KEY;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        Long campaignId = Long.valueOf(event.getBusinessKey());
        MarketingCampaignDO campaign = campaignMapper.selectById(campaignId);
        if (campaign == null) {
            log.warn("[onEvent][营销活动({})不存在]", campaignId);
            return;
        }
        // 幂等校验：只有待审核状态才处理
        if (!CampaignStatusEnum.isPendingApproval(campaign.getStatus())) {
            log.warn("[onEvent][营销活动({})当前状态({})不是待审核，忽略重复回调]", campaignId, campaign.getStatus());
            return;
        }
        // 根据 BPM 审批结果更新状态
        Integer eventStatus = event.getStatus();
        if (BpmProcessInstanceStatusEnum.isRejectStatus(eventStatus)) {
            // 审批拒绝 → 退回草稿
            campaign.setStatus(CampaignStatusEnum.DRAFT.getStatus());
            log.info("[onEvent][营销活动({})审批拒绝，退回草稿]", campaignId);
        } else if (BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(eventStatus)) {
            // 审批通过 → 已审核
            campaign.setStatus(CampaignStatusEnum.APPROVED.getStatus());
            log.info("[onEvent][营销活动({})审批通过]", campaignId);
        } else if (BpmProcessInstanceStatusEnum.CANCEL.getStatus().equals(eventStatus)) {
            // 流程取消 → 已终止
            campaign.setStatus(CampaignStatusEnum.CANCELLED.getStatus());
            log.info("[onEvent][营销活动({})审批取消]", campaignId);
        } else {
            log.info("[onEvent][营销活动({})收到非终结事件({})，跳过]", campaignId, eventStatus);
            return;
        }
        campaignMapper.updateById(campaign);
    }

}

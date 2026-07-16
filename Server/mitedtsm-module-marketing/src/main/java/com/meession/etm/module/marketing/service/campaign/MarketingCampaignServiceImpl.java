package com.meession.etm.module.marketing.service.campaign;

import cn.hutool.core.util.StrUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.marketing.controller.admin.campaign.vo.CampaignPageReqVO;
import com.meession.etm.module.marketing.controller.admin.campaign.vo.CampaignSaveReqVO;
import com.meession.etm.module.marketing.dal.dataobject.campaign.MarketingCampaignDO;
import com.meession.etm.module.marketing.dal.mysql.campaign.MarketingCampaignMapper;
import com.meession.etm.module.marketing.enums.CampaignStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.marketing.enums.ErrorCodeConstants.*;

/**
 * 营销活动 Service 实现类
 *
 * @author MITEDTSM
 */
@Service
@Slf4j
public class MarketingCampaignServiceImpl implements MarketingCampaignService {

    @Resource
    private MarketingCampaignMapper campaignMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCampaign(CampaignSaveReqVO createReqVO) {
        // 校验活动名称是否重复
        validateCampaignNameDuplicate(null, createReqVO.getName());
        // 插入
        MarketingCampaignDO campaign = BeanUtils.toBean(createReqVO, MarketingCampaignDO.class);
        campaign.setStatus(CampaignStatusEnum.DRAFT.getStatus());
        campaign.setSentCount(0);
        campaign.setSuccessCount(0);
        campaign.setFailCount(0);
        campaignMapper.insert(campaign);
        return campaign.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCampaign(CampaignSaveReqVO updateReqVO) {
        // 校验存在
        validateCampaignExists(updateReqVO.getId());
        // 校验活动名称是否重复
        validateCampaignNameDuplicate(updateReqVO.getId(), updateReqVO.getName());
        // 只有草稿状态才能编辑
        MarketingCampaignDO existCampaign = campaignMapper.selectById(updateReqVO.getId());
        if (!CampaignStatusEnum.isDraft(existCampaign.getStatus())) {
            throw exception(CAMPAIGN_STATUS_NOT_DRAFT);
        }
        // 更新
        MarketingCampaignDO updateObj = BeanUtils.toBean(updateReqVO, MarketingCampaignDO.class);
        campaignMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCampaign(Long id) {
        // 校验存在
        validateCampaignExists(id);
        // 只有草稿状态才能删除
        MarketingCampaignDO existCampaign = campaignMapper.selectById(id);
        if (!CampaignStatusEnum.isDraft(existCampaign.getStatus())) {
            throw exception(CAMPAIGN_STATUS_NOT_DRAFT);
        }
        // 删除
        campaignMapper.deleteById(id);
    }

    @Override
    public MarketingCampaignDO getCampaign(Long id) {
        return campaignMapper.selectById(id);
    }

    @Override
    public PageResult<MarketingCampaignDO> getCampaignPage(CampaignPageReqVO pageReqVO) {
        return campaignMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submitApproval(Long id, Long userId) {
        // 校验存在
        validateCampaignExists(id);
        MarketingCampaignDO campaign = campaignMapper.selectById(id);
        // 只有草稿状态才能提交审核
        if (!CampaignStatusEnum.isDraft(campaign.getStatus())) {
            throw exception(CAMPAIGN_STATUS_NOT_DRAFT);
        }
        // 更新为待审核状态
        campaign.setStatus(CampaignStatusEnum.PENDING_APPROVAL.getStatus());
        // TODO: 创建 BPM 流程实例，关联 module-bpm
        // String processInstanceId = bpmProcessInstanceApi.createProcessInstance(...);
        // campaign.setBpmProcessInstanceId(processInstanceId);
        campaignMapper.updateById(campaign);
        return campaign.getBpmProcessInstanceId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startCampaign(Long id) {
        // 校验存在
        validateCampaignExists(id);
        MarketingCampaignDO campaign = campaignMapper.selectById(id);
        // 只有已审核状态才能启动
        if (!CampaignStatusEnum.isApproved(campaign.getStatus())) {
            throw exception(CAMPAIGN_STATUS_NOT_APPROVED);
        }
        // 更新为进行中状态
        campaign.setStatus(CampaignStatusEnum.IN_PROGRESS.getStatus());
        campaign.setSendTime(LocalDateTime.now());
        campaignMapper.updateById(campaign);
        // TODO: 根据活动类型和模板执行批量发送逻辑
        // if (CampaignTypeEnum.SMS.getType().equals(campaign.getType())) {
        //     smsBatchSendService.batchSend(campaign);
        // } else if (CampaignTypeEnum.MAIL.getType().equals(campaign.getType())) {
        //     mailBatchSendService.batchSend(campaign);
        // }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelCampaign(Long id) {
        // 校验存在
        validateCampaignExists(id);
        MarketingCampaignDO campaign = campaignMapper.selectById(id);
        // 草稿和已审核状态可以取消
        if (!CampaignStatusEnum.isDraft(campaign.getStatus())
                && !CampaignStatusEnum.isApproved(campaign.getStatus())
                && !CampaignStatusEnum.isPendingApproval(campaign.getStatus())) {
            throw exception(CAMPAIGN_STATUS_NOT_DRAFT);
        }
        // 更新为已终止状态
        campaign.setStatus(CampaignStatusEnum.CANCELLED.getStatus());
        campaign.setEndTime(LocalDateTime.now());
        campaignMapper.updateById(campaign);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSendStatistics(Long id, Integer sentCount, Integer successCount, Integer failCount) {
        MarketingCampaignDO campaign = campaignMapper.selectById(id);
        if (campaign == null) {
            return;
        }
        campaign.setSentCount((campaign.getSentCount() == null ? 0 : campaign.getSentCount()) + sentCount);
        campaign.setSuccessCount((campaign.getSuccessCount() == null ? 0 : campaign.getSuccessCount()) + successCount);
        campaign.setFailCount((campaign.getFailCount() == null ? 0 : campaign.getFailCount()) + failCount);
        campaignMapper.updateById(campaign);
    }

    /**
     * 校验营销活动是否存在
     */
    private void validateCampaignExists(Long id) {
        if (campaignMapper.selectById(id) == null) {
            throw exception(CAMPAIGN_NOT_EXISTS);
        }
    }

    /**
     * 校验营销活动名称是否重复
     */
    private void validateCampaignNameDuplicate(Long id, String name) {
        if (StrUtil.isBlank(name)) {
            return;
        }
        MarketingCampaignDO campaign = campaignMapper.selectByName(name);
        if (campaign == null) {
            return;
        }
        if (id == null || !campaign.getId().equals(id)) {
            throw exception(CAMPAIGN_NAME_DUPLICATE, name);
        }
    }

}

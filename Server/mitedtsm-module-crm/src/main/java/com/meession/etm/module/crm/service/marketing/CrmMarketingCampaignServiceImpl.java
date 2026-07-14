package com.meession.etm.module.crm.service.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.marketing.vo.campaign.CrmMarketingCampaignPageReqVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.campaign.CrmMarketingCampaignSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingCampaignDO;
import com.meession.etm.module.crm.dal.mysql.marketing.CrmMarketingCampaignMapper;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingCampaignStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

/**
 * 营销活动 Service 实现类
 *
 * @author mitedtsm
 */
@Service
@Validated
public class CrmMarketingCampaignServiceImpl implements CrmMarketingCampaignService {

    @Resource
    private CrmMarketingCampaignMapper campaignMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCampaign(CrmMarketingCampaignSaveReqVO createReqVO) {
        CrmMarketingCampaignDO campaign = BeanUtils.toBean(createReqVO, CrmMarketingCampaignDO.class);
        // 默认状态为草稿
        if (campaign.getStatus() == null) {
            campaign.setStatus(CrmMarketingCampaignStatusEnum.DRAFT.getStatus());
        }
        campaignMapper.insert(campaign);
        return campaign.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCampaign(CrmMarketingCampaignSaveReqVO updateReqVO) {
        // 校验存在
        CrmMarketingCampaignDO existCampaign = validateCampaign(updateReqVO.getId());
        // 只有草稿状态可编辑
        if (!CrmMarketingCampaignStatusEnum.DRAFT.getStatus().equals(existCampaign.getStatus())) {
            throw exception(CAMPAIGN_STATUS_NOT_DRAFT);
        }
        CrmMarketingCampaignDO updateObj = BeanUtils.toBean(updateReqVO, CrmMarketingCampaignDO.class);
        campaignMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCampaign(Long id) {
        // 校验存在
        CrmMarketingCampaignDO existCampaign = validateCampaign(id);
        // 进行中的活动不能删除
        if (CrmMarketingCampaignStatusEnum.RUNNING.getStatus().equals(existCampaign.getStatus())) {
            throw exception(CAMPAIGN_DELETE_FAIL_RUNNING);
        }
        campaignMapper.deleteById(id);
    }

    @Override
    public CrmMarketingCampaignDO getCampaign(Long id) {
        return campaignMapper.selectById(id);
    }

    @Override
    public List<CrmMarketingCampaignDO> getCampaignList(Collection<Long> ids) {
        return campaignMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<CrmMarketingCampaignDO> getCampaignPage(CrmMarketingCampaignPageReqVO pageReqVO) {
        return campaignMapper.selectPage(pageReqVO);
    }

    /**
     * 校验营销活动是否存在
     *
     * @param id 编号
     * @return 营销活动
     */
    private CrmMarketingCampaignDO validateCampaign(Long id) {
        CrmMarketingCampaignDO campaign = campaignMapper.selectById(id);
        if (campaign == null) {
            throw exception(CAMPAIGN_NOT_EXISTS);
        }
        return campaign;
    }

}

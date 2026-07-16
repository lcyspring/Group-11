package com.meession.etm.module.marketing.service.log;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.marketing.controller.admin.log.vo.SendLogPageReqVO;
import com.meession.etm.module.marketing.controller.admin.log.vo.SendStatisticsRespVO;
import com.meession.etm.module.marketing.dal.dataobject.campaign.MarketingCampaignDO;
import com.meession.etm.module.marketing.dal.dataobject.log.MarketingSendRecordDO;
import com.meession.etm.module.marketing.dal.mysql.campaign.MarketingCampaignMapper;
import com.meession.etm.module.marketing.dal.mysql.log.MarketingSendRecordMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 营销发送记录 Service 实现类
 *
 * @author MITEDTSM
 */
@Service
public class MarketingSendLogServiceImpl implements MarketingSendLogService {

    @Resource
    private MarketingSendRecordMapper sendRecordMapper;

    @Resource
    private MarketingCampaignMapper campaignMapper;

    @Override
    public PageResult<MarketingSendRecordDO> getSendLogPage(SendLogPageReqVO pageReqVO) {
        return sendRecordMapper.selectPage(pageReqVO);
    }

    @Override
    public MarketingSendRecordDO getSendLog(Long id) {
        return sendRecordMapper.selectById(id);
    }

    @Override
    public SendStatisticsRespVO getStatistics(Long campaignId) {
        MarketingCampaignDO campaign = campaignMapper.selectById(campaignId);
        SendStatisticsRespVO vo = new SendStatisticsRespVO();
        vo.setCampaignId(campaignId);
        if (campaign != null) {
            vo.setCampaignName(campaign.getName());
            vo.setTotalSent((long) (campaign.getSentCount() == null ? 0 : campaign.getSentCount()));
        }
        vo.setSmsSent(sendRecordMapper.selectCountByCampaignIdAndChannel(campaignId, "SMS"));
        vo.setMailSent(sendRecordMapper.selectCountByCampaignIdAndChannel(campaignId, "MAIL"));
        return vo;
    }

}

package com.meession.etm.module.marketing.service.effect;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectChannelRespVO;
import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectFunnelRespVO;
import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectRankingRespVO;
import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectReqVO;
import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectSummaryRespVO;
import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectTrendRespVO;
import com.meession.etm.module.marketing.dal.dataobject.campaign.MarketingCampaignDO;
import com.meession.etm.module.marketing.dal.mysql.campaign.MarketingCampaignMapper;
import com.meession.etm.module.marketing.enums.CampaignTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 营销活动效果分析 Service 实现类
 * <p>
 * 当前优先复用 marketing_campaign 中的发送统计字段。线索、客户、商机、成交等转化指标
 * 预留为 0，后续在 CRM 链路补齐 campaignId 归因后再接入真实数据。
 *
 * @author MITEDTSM
 */
@Service
public class CampaignEffectServiceImpl implements CampaignEffectService {

    private static final BigDecimal ZERO_RATE = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Resource
    private MarketingCampaignMapper campaignMapper;

    @Override
    public CampaignEffectSummaryRespVO getSummary(CampaignEffectReqVO reqVO) {
        List<MarketingCampaignDO> campaigns = listCampaigns(reqVO);
        Metrics metrics = sum(campaigns);

        CampaignEffectSummaryRespVO vo = new CampaignEffectSummaryRespVO();
        vo.setCampaignCount((long) campaigns.size());
        vo.setSendCount(metrics.sendCount);
        vo.setDeliveryCount(metrics.deliveryCount);
        vo.setFailCount(metrics.failCount);
        vo.setDeliveryRate(rate(metrics.deliveryCount, metrics.sendCount));
        vo.setResponseCount(0L);
        vo.setLeadCount(0L);
        vo.setCustomerCount(0L);
        vo.setOpportunityCount(0L);
        vo.setDealCount(0L);
        vo.setConversionRate(ZERO_RATE);
        return vo;
    }

    @Override
    public List<CampaignEffectTrendRespVO> getTrend(CampaignEffectReqVO reqVO) {
        List<MarketingCampaignDO> campaigns = listCampaigns(reqVO);
        if (CollUtil.isEmpty(campaigns)) {
            return new ArrayList<>();
        }
        Map<LocalDate, List<MarketingCampaignDO>> groupMap = campaigns.stream()
                .collect(Collectors.groupingBy(campaign -> getStatDate(campaign), LinkedHashMap::new, Collectors.toList()));
        return groupMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Metrics metrics = sum(entry.getValue());
                    CampaignEffectTrendRespVO vo = new CampaignEffectTrendRespVO();
                    vo.setDate(entry.getKey().toString());
                    vo.setCampaignCount((long) entry.getValue().size());
                    vo.setSendCount(metrics.sendCount);
                    vo.setDeliveryCount(metrics.deliveryCount);
                    vo.setFailCount(metrics.failCount);
                    vo.setDeliveryRate(rate(metrics.deliveryCount, metrics.sendCount));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CampaignEffectRankingRespVO> getRanking(CampaignEffectReqVO reqVO) {
        return listCampaigns(reqVO).stream()
                .map(this::toRankingRespVO)
                .sorted(Comparator.comparing(CampaignEffectRankingRespVO::getDeliveryCount, Comparator.nullsFirst(Long::compareTo)).reversed()
                        .thenComparing(Comparator.comparing(CampaignEffectRankingRespVO::getSendCount, Comparator.nullsFirst(Long::compareTo)).reversed()))
                .limit(20)
                .collect(Collectors.toList());
    }

    @Override
    public CampaignEffectFunnelRespVO getFunnel(CampaignEffectReqVO reqVO) {
        Metrics metrics = sum(listCampaigns(reqVO));
        CampaignEffectFunnelRespVO vo = new CampaignEffectFunnelRespVO();
        vo.setSendCount(metrics.sendCount);
        vo.setDeliveryCount(metrics.deliveryCount);
        vo.setResponseCount(0L);
        vo.setLeadCount(0L);
        vo.setCustomerCount(0L);
        vo.setOpportunityCount(0L);
        vo.setDealCount(0L);
        vo.setDeliveryRate(rate(metrics.deliveryCount, metrics.sendCount));
        vo.setLeadConversionRate(ZERO_RATE);
        vo.setDealConversionRate(ZERO_RATE);
        return vo;
    }

    @Override
    public List<CampaignEffectChannelRespVO> getChannel(CampaignEffectReqVO reqVO) {
        Map<String, List<MarketingCampaignDO>> groupMap = listCampaigns(reqVO).stream()
                .collect(Collectors.groupingBy(campaign -> getChannel(campaign.getType()), LinkedHashMap::new, Collectors.toList()));
        return groupMap.entrySet().stream()
                .map(entry -> {
                    Metrics metrics = sum(entry.getValue());
                    CampaignEffectChannelRespVO vo = new CampaignEffectChannelRespVO();
                    vo.setChannel(entry.getKey());
                    vo.setCampaignCount((long) entry.getValue().size());
                    vo.setSendCount(metrics.sendCount);
                    vo.setDeliveryCount(metrics.deliveryCount);
                    vo.setFailCount(metrics.failCount);
                    vo.setDeliveryRate(rate(metrics.deliveryCount, metrics.sendCount));
                    vo.setLeadCount(0L);
                    vo.setDealCount(0L);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private List<MarketingCampaignDO> listCampaigns(CampaignEffectReqVO reqVO) {
        List<MarketingCampaignDO> campaigns = campaignMapper.selectList(reqVO);
        if (StrUtil.isBlank(reqVO.getChannel())) {
            return campaigns;
        }
        return campaigns.stream()
                .filter(campaign -> reqVO.getChannel().equals(getChannel(campaign.getType())))
                .collect(Collectors.toList());
    }

    private CampaignEffectRankingRespVO toRankingRespVO(MarketingCampaignDO campaign) {
        Long sendCount = value(campaign.getSentCount());
        Long deliveryCount = value(campaign.getSuccessCount());
        Long failCount = value(campaign.getFailCount());

        CampaignEffectRankingRespVO vo = new CampaignEffectRankingRespVO();
        vo.setCampaignId(campaign.getId());
        vo.setCampaignName(campaign.getName());
        vo.setType(campaign.getType());
        vo.setChannel(getChannel(campaign.getType()));
        vo.setSendCount(sendCount);
        vo.setDeliveryCount(deliveryCount);
        vo.setFailCount(failCount);
        vo.setDeliveryRate(rate(deliveryCount, sendCount));
        vo.setLeadCount(0L);
        vo.setDealCount(0L);
        vo.setConversionRate(ZERO_RATE);
        return vo;
    }

    private Metrics sum(List<MarketingCampaignDO> campaigns) {
        Metrics metrics = new Metrics();
        if (CollUtil.isEmpty(campaigns)) {
            return metrics;
        }
        for (MarketingCampaignDO campaign : campaigns) {
            metrics.sendCount += value(campaign.getSentCount());
            metrics.deliveryCount += value(campaign.getSuccessCount());
            metrics.failCount += value(campaign.getFailCount());
        }
        return metrics;
    }

    private Long value(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private BigDecimal rate(Long numerator, Long denominator) {
        if (denominator == null || denominator == 0) {
            return ZERO_RATE;
        }
        return BigDecimal.valueOf(numerator == null ? 0 : numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private LocalDate getStatDate(MarketingCampaignDO campaign) {
        if (campaign.getSendTime() != null) {
            return campaign.getSendTime().toLocalDate();
        }
        if (campaign.getCreateTime() != null) {
            return campaign.getCreateTime().toLocalDate();
        }
        return LocalDate.now();
    }

    private String getChannel(Integer type) {
        if (CampaignTypeEnum.isSms(type)) {
            return "SMS";
        }
        if (CampaignTypeEnum.isMail(type)) {
            return "MAIL";
        }
        return "UNKNOWN";
    }

    private static class Metrics {
        private Long sendCount = 0L;
        private Long deliveryCount = 0L;
        private Long failCount = 0L;
    }

}

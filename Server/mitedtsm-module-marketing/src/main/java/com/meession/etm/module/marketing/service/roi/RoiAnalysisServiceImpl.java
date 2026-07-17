package com.meession.etm.module.marketing.service.roi;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisCampaignRankingRespVO;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisChannelRespVO;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisFunnelRespVO;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisReqVO;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisSummaryRespVO;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisTrendRespVO;
import com.meession.etm.module.marketing.dal.dataobject.campaign.MarketingCampaignDO;
import com.meession.etm.module.marketing.dal.dataobject.roi.MarketingCampaignRoiDO;
import com.meession.etm.module.marketing.dal.mysql.campaign.MarketingCampaignMapper;
import com.meession.etm.module.marketing.dal.mysql.roi.MarketingCampaignRoiMapper;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 营销 ROI 分析 Service 实现类
 *
 * @author MITEDTSM
 */
@Service
public class RoiAnalysisServiceImpl implements RoiAnalysisService {

    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Resource
    private MarketingCampaignRoiMapper roiMapper;

    @Resource
    private MarketingCampaignMapper campaignMapper;

    @Override
    public RoiAnalysisSummaryRespVO getSummary(RoiAnalysisReqVO reqVO) {
        List<MarketingCampaignRoiDO> rows = roiMapper.selectList(reqVO);
        RoiMetrics metrics = sum(rows);

        RoiAnalysisSummaryRespVO vo = new RoiAnalysisSummaryRespVO();
        vo.setCampaignCount(rows.stream().map(MarketingCampaignRoiDO::getCampaignId).filter(Objects::nonNull).distinct().count());
        vo.setTotalCost(metrics.cost);
        vo.setTotalRevenue(metrics.revenue);
        vo.setGrossProfit(metrics.revenue.subtract(metrics.cost).setScale(2, RoundingMode.HALF_UP));
        vo.setRoi(roi(metrics.revenue, metrics.cost));
        vo.setRoas(roas(metrics.revenue, metrics.cost));
        vo.setLeadCount(metrics.leadCount);
        vo.setCustomerCount(metrics.customerCount);
        vo.setOpportunityCount(metrics.opportunityCount);
        vo.setDealCount(metrics.dealCount);
        vo.setConversionRate(rate(metrics.dealCount, metrics.leadCount));
        vo.setCostPerLead(amountPer(metrics.cost, metrics.leadCount));
        vo.setCostPerDeal(amountPer(metrics.cost, metrics.dealCount));
        return vo;
    }

    @Override
    public List<RoiAnalysisTrendRespVO> getTrend(RoiAnalysisReqVO reqVO) {
        List<MarketingCampaignRoiDO> rows = roiMapper.selectList(reqVO);
        if (CollUtil.isEmpty(rows)) {
            return new ArrayList<>();
        }
        Map<LocalDate, List<MarketingCampaignRoiDO>> groupMap = rows.stream()
                .collect(Collectors.groupingBy(row -> row.getStatDate() == null ? LocalDate.now() : row.getStatDate(), LinkedHashMap::new, Collectors.toList()));
        return groupMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    RoiMetrics metrics = sum(entry.getValue());
                    RoiAnalysisTrendRespVO vo = new RoiAnalysisTrendRespVO();
                    vo.setDate(entry.getKey().toString());
                    vo.setCost(metrics.cost);
                    vo.setRevenue(metrics.revenue);
                    vo.setRoi(roi(metrics.revenue, metrics.cost));
                    vo.setRoas(roas(metrics.revenue, metrics.cost));
                    vo.setLeadCount(metrics.leadCount);
                    vo.setDealCount(metrics.dealCount);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<RoiAnalysisCampaignRankingRespVO> getCampaignRanking(RoiAnalysisReqVO reqVO) {
        Map<Long, List<MarketingCampaignRoiDO>> groupMap = roiMapper.selectList(reqVO).stream()
                .filter(row -> row.getCampaignId() != null)
                .collect(Collectors.groupingBy(MarketingCampaignRoiDO::getCampaignId, LinkedHashMap::new, Collectors.toList()));
        return groupMap.entrySet().stream()
                .map(entry -> toCampaignRanking(entry.getKey(), sum(entry.getValue())))
                .sorted(Comparator.comparing(RoiAnalysisCampaignRankingRespVO::getRoi, Comparator.nullsFirst(BigDecimal::compareTo)).reversed())
                .limit(20)
                .collect(Collectors.toList());
    }

    @Override
    public List<RoiAnalysisChannelRespVO> getChannel(RoiAnalysisReqVO reqVO) {
        Map<String, List<MarketingCampaignRoiDO>> groupMap = roiMapper.selectList(reqVO).stream()
                .collect(Collectors.groupingBy(row -> StrUtil.blankToDefault(row.getChannel(), "UNKNOWN"), LinkedHashMap::new, Collectors.toList()));
        return groupMap.entrySet().stream()
                .map(entry -> {
                    RoiMetrics metrics = sum(entry.getValue());
                    RoiAnalysisChannelRespVO vo = new RoiAnalysisChannelRespVO();
                    vo.setChannel(entry.getKey());
                    vo.setCost(metrics.cost);
                    vo.setRevenue(metrics.revenue);
                    vo.setRoi(roi(metrics.revenue, metrics.cost));
                    vo.setRoas(roas(metrics.revenue, metrics.cost));
                    vo.setLeadCount(metrics.leadCount);
                    vo.setDealCount(metrics.dealCount);
                    return vo;
                })
                .sorted(Comparator.comparing(RoiAnalysisChannelRespVO::getRevenue, Comparator.nullsFirst(BigDecimal::compareTo)).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public RoiAnalysisFunnelRespVO getFunnel(RoiAnalysisReqVO reqVO) {
        List<MarketingCampaignRoiDO> rows = roiMapper.selectList(reqVO);
        RoiMetrics metrics = sum(rows);
        SendMetrics sendMetrics = sumSendMetrics(rows, reqVO);

        RoiAnalysisFunnelRespVO vo = new RoiAnalysisFunnelRespVO();
        vo.setSendCount(sendMetrics.sendCount);
        vo.setDeliveryCount(sendMetrics.deliveryCount);
        vo.setLeadCount(metrics.leadCount);
        vo.setCustomerCount(metrics.customerCount);
        vo.setOpportunityCount(metrics.opportunityCount);
        vo.setDealCount(metrics.dealCount);
        vo.setDeliveryRate(rate(sendMetrics.deliveryCount, sendMetrics.sendCount));
        vo.setLeadConversionRate(rate(metrics.leadCount, sendMetrics.deliveryCount));
        vo.setCustomerConversionRate(rate(metrics.customerCount, metrics.leadCount));
        vo.setDealConversionRate(rate(metrics.dealCount, metrics.customerCount));
        return vo;
    }

    private RoiAnalysisCampaignRankingRespVO toCampaignRanking(Long campaignId, RoiMetrics metrics) {
        MarketingCampaignDO campaign = campaignMapper.selectById(campaignId);
        RoiAnalysisCampaignRankingRespVO vo = new RoiAnalysisCampaignRankingRespVO();
        vo.setCampaignId(campaignId);
        vo.setCampaignName(campaign == null ? null : campaign.getName());
        vo.setCost(metrics.cost);
        vo.setRevenue(metrics.revenue);
        vo.setRoi(roi(metrics.revenue, metrics.cost));
        vo.setRoas(roas(metrics.revenue, metrics.cost));
        vo.setLeadCount(metrics.leadCount);
        vo.setDealCount(metrics.dealCount);
        return vo;
    }

    private RoiMetrics sum(List<MarketingCampaignRoiDO> rows) {
        RoiMetrics metrics = new RoiMetrics();
        if (CollUtil.isEmpty(rows)) {
            return metrics;
        }
        for (MarketingCampaignRoiDO row : rows) {
            metrics.cost = metrics.cost.add(amount(row.getCostAmount()));
            metrics.revenue = metrics.revenue.add(amount(row.getRevenueAmount()));
            metrics.leadCount += value(row.getLeadCount());
            metrics.customerCount += value(row.getCustomerCount());
            metrics.opportunityCount += value(row.getOpportunityCount());
            metrics.dealCount += value(row.getDealCount());
        }
        metrics.cost = metrics.cost.setScale(2, RoundingMode.HALF_UP);
        metrics.revenue = metrics.revenue.setScale(2, RoundingMode.HALF_UP);
        return metrics;
    }

    private SendMetrics sumSendMetrics(List<MarketingCampaignRoiDO> rows, RoiAnalysisReqVO reqVO) {
        SendMetrics metrics = new SendMetrics();
        Set<Long> campaignIds = rows.stream().map(MarketingCampaignRoiDO::getCampaignId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (CollUtil.isEmpty(campaignIds) && reqVO.getCampaignId() != null) {
            campaignIds.add(reqVO.getCampaignId());
        }
        for (Long campaignId : campaignIds) {
            MarketingCampaignDO campaign = campaignMapper.selectById(campaignId);
            if (campaign == null) {
                continue;
            }
            if (StrUtil.isNotBlank(reqVO.getChannel()) && !reqVO.getChannel().equals(getChannel(campaign.getType()))) {
                continue;
            }
            metrics.sendCount += value(campaign.getSentCount());
            metrics.deliveryCount += value(campaign.getSuccessCount());
        }
        return metrics;
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? ZERO_AMOUNT : value;
    }

    private Long value(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private BigDecimal roi(BigDecimal revenue, BigDecimal cost) {
        if (cost == null || cost.compareTo(BigDecimal.ZERO) == 0) {
            return ZERO_AMOUNT;
        }
        return revenue.subtract(cost).multiply(BigDecimal.valueOf(100)).divide(cost, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal roas(BigDecimal revenue, BigDecimal cost) {
        if (cost == null || cost.compareTo(BigDecimal.ZERO) == 0) {
            return ZERO_AMOUNT;
        }
        return revenue.divide(cost, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(Long numerator, Long denominator) {
        if (denominator == null || denominator == 0) {
            return ZERO_AMOUNT;
        }
        return BigDecimal.valueOf(numerator == null ? 0 : numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal amountPer(BigDecimal amount, Long count) {
        if (count == null || count == 0) {
            return ZERO_AMOUNT;
        }
        return amount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
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

    private static class RoiMetrics {
        private BigDecimal cost = ZERO_AMOUNT;
        private BigDecimal revenue = ZERO_AMOUNT;
        private Long leadCount = 0L;
        private Long customerCount = 0L;
        private Long opportunityCount = 0L;
        private Long dealCount = 0L;
    }

    private static class SendMetrics {
        private Long sendCount = 0L;
        private Long deliveryCount = 0L;
    }

}

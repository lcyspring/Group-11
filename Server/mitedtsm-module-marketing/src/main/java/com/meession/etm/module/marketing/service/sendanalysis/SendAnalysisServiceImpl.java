package com.meession.etm.module.marketing.service.sendanalysis;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisChannelRespVO;
import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisFailReasonRespVO;
import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisReqVO;
import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisSummaryRespVO;
import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisTemplateRespVO;
import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisTrendRespVO;
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
 * 营销发送分析 Service 实现类
 * <p>
 * 当前复用 marketing_campaign 的 sent_count / success_count / fail_count 汇总字段。
 * 若后续接入短信/邮件服务商回执，可扩展为读取明细发送日志并补充失败原因。
 *
 * @author MITEDTSM
 */
@Service
public class SendAnalysisServiceImpl implements SendAnalysisService {

    private static final BigDecimal ZERO_RATE = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Resource
    private MarketingCampaignMapper campaignMapper;

    @Override
    public SendAnalysisSummaryRespVO getSummary(SendAnalysisReqVO reqVO) {
        List<MarketingCampaignDO> campaigns = listCampaigns(reqVO);
        Metrics metrics = sum(campaigns);

        SendAnalysisSummaryRespVO vo = new SendAnalysisSummaryRespVO();
        vo.setCampaignCount((long) campaigns.size());
        vo.setTotalCount(metrics.totalCount);
        vo.setSuccessCount(metrics.successCount);
        vo.setFailCount(metrics.failCount);
        vo.setPendingCount(metrics.pendingCount);
        vo.setDeliveryRate(rate(metrics.successCount, metrics.totalCount));
        vo.setFailRate(rate(metrics.failCount, metrics.totalCount));
        return vo;
    }

    @Override
    public List<SendAnalysisTrendRespVO> getTrend(SendAnalysisReqVO reqVO) {
        List<MarketingCampaignDO> campaigns = listCampaigns(reqVO);
        if (CollUtil.isEmpty(campaigns)) {
            return new ArrayList<>();
        }
        Map<LocalDate, List<MarketingCampaignDO>> groupMap = campaigns.stream()
                .collect(Collectors.groupingBy(this::getStatDate, LinkedHashMap::new, Collectors.toList()));
        return groupMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Metrics metrics = sum(entry.getValue());
                    SendAnalysisTrendRespVO vo = new SendAnalysisTrendRespVO();
                    vo.setDate(entry.getKey().toString());
                    vo.setTotalCount(metrics.totalCount);
                    vo.setSuccessCount(metrics.successCount);
                    vo.setFailCount(metrics.failCount);
                    vo.setPendingCount(metrics.pendingCount);
                    vo.setDeliveryRate(rate(metrics.successCount, metrics.totalCount));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<SendAnalysisChannelRespVO> getChannel(SendAnalysisReqVO reqVO) {
        Map<String, List<MarketingCampaignDO>> groupMap = listCampaigns(reqVO).stream()
                .collect(Collectors.groupingBy(campaign -> getChannel(campaign.getType()), LinkedHashMap::new, Collectors.toList()));
        return groupMap.entrySet().stream()
                .map(entry -> {
                    Metrics metrics = sum(entry.getValue());
                    SendAnalysisChannelRespVO vo = new SendAnalysisChannelRespVO();
                    vo.setChannel(entry.getKey());
                    vo.setTotalCount(metrics.totalCount);
                    vo.setSuccessCount(metrics.successCount);
                    vo.setFailCount(metrics.failCount);
                    vo.setDeliveryRate(rate(metrics.successCount, metrics.totalCount));
                    return vo;
                })
                .sorted(Comparator.comparing(SendAnalysisChannelRespVO::getTotalCount, Comparator.nullsFirst(Long::compareTo)).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<SendAnalysisTemplateRespVO> getTemplate(SendAnalysisReqVO reqVO) {
        Map<TemplateKey, List<MarketingCampaignDO>> groupMap = listCampaigns(reqVO).stream()
                .collect(Collectors.groupingBy(campaign -> new TemplateKey(campaign.getTemplateId(), getChannel(campaign.getType())), LinkedHashMap::new, Collectors.toList()));
        return groupMap.entrySet().stream()
                .map(entry -> {
                    Metrics metrics = sum(entry.getValue());
                    SendAnalysisTemplateRespVO vo = new SendAnalysisTemplateRespVO();
                    vo.setTemplateId(entry.getKey().templateId);
                    vo.setChannel(entry.getKey().channel);
                    vo.setTotalCount(metrics.totalCount);
                    vo.setSuccessCount(metrics.successCount);
                    vo.setFailCount(metrics.failCount);
                    vo.setDeliveryRate(rate(metrics.successCount, metrics.totalCount));
                    return vo;
                })
                .sorted(Comparator.comparing(SendAnalysisTemplateRespVO::getTotalCount, Comparator.nullsFirst(Long::compareTo)).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<SendAnalysisFailReasonRespVO> getFailReason(SendAnalysisReqVO reqVO) {
        Metrics metrics = sum(listCampaigns(reqVO));
        if (metrics.failCount == 0) {
            return new ArrayList<>();
        }
        SendAnalysisFailReasonRespVO vo = new SendAnalysisFailReasonRespVO();
        vo.setReason("UNKNOWN");
        vo.setCount(metrics.failCount);
        vo.setRate(rate(metrics.failCount, metrics.failCount));
        return List.of(vo);
    }

    private List<MarketingCampaignDO> listCampaigns(SendAnalysisReqVO reqVO) {
        List<MarketingCampaignDO> campaigns = campaignMapper.selectList(reqVO);
        if (StrUtil.isBlank(reqVO.getChannel())) {
            return campaigns;
        }
        return campaigns.stream()
                .filter(campaign -> reqVO.getChannel().equals(getChannel(campaign.getType())))
                .collect(Collectors.toList());
    }

    private Metrics sum(List<MarketingCampaignDO> campaigns) {
        Metrics metrics = new Metrics();
        if (CollUtil.isEmpty(campaigns)) {
            return metrics;
        }
        for (MarketingCampaignDO campaign : campaigns) {
            metrics.totalCount += value(campaign.getSentCount());
            metrics.successCount += value(campaign.getSuccessCount());
            metrics.failCount += value(campaign.getFailCount());
        }
        metrics.pendingCount = Math.max(metrics.totalCount - metrics.successCount - metrics.failCount, 0L);
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
        private Long totalCount = 0L;
        private Long successCount = 0L;
        private Long failCount = 0L;
        private Long pendingCount = 0L;
    }

    private static class TemplateKey {
        private final Long templateId;
        private final String channel;

        private TemplateKey(Long templateId, String channel) {
            this.templateId = templateId;
            this.channel = channel;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TemplateKey)) {
                return false;
            }
            TemplateKey that = (TemplateKey) o;
            return java.util.Objects.equals(templateId, that.templateId) && java.util.Objects.equals(channel, that.channel);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(templateId, channel);
        }
    }

}

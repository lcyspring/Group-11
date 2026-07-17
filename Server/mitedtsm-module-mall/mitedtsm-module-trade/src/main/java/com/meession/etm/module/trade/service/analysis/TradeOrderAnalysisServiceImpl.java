package com.meession.etm.module.trade.service.analysis;

import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.trade.controller.admin.analysis.vo.TradeOrderAnalysisReqVO;
import com.meession.etm.module.trade.controller.admin.analysis.vo.TradeOrderAnalysisRespVO;
import com.meession.etm.module.trade.controller.admin.analysis.vo.TradeOrderAnalysisRespVO.*;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderMapper;
import com.meession.etm.module.trade.enums.order.TradeOrderStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class TradeOrderAnalysisServiceImpl implements TradeOrderAnalysisService {

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Override
    public TradeOrderAnalysisRespVO getOrderAnalysis(TradeOrderAnalysisReqVO reqVO) {
        TradeOrderAnalysisRespVO result = new TradeOrderAnalysisRespVO();

        result.setTrend(analyzeTrend(reqVO));
        result.setConversion(analyzeConversion(reqVO));
        result.setAmountDistribution(analyzeAmountDistribution(reqVO));
        result.setStatusDistribution(analyzeStatusDistribution(reqVO));

        return result;
    }

    private TrendAnalysis analyzeTrend(TradeOrderAnalysisReqVO reqVO) {
        TrendAnalysis trend = new TrendAnalysis();
        List<String> dates = new ArrayList<>();
        List<Integer> orderCountTrend = new ArrayList<>();
        List<Long> amountTrend = new ArrayList<>();
        List<Long> paidAmountTrend = new ArrayList<>();

        LocalDateTime start = reqVO.getStartTime() != null ? reqVO.getStartTime() : LocalDateTime.now().minusDays(30);
        LocalDateTime end = reqVO.getEndTime() != null ? reqVO.getEndTime() : LocalDateTime.now();

        LocalDate currentDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate.format(formatter));

            LocalDateTime dayStart = currentDate.atStartOfDay();
            LocalDateTime dayEnd = currentDate.atTime(23, 59, 59);

            LambdaQueryWrapperX<TradeOrderDO> wrapper = new LambdaQueryWrapperX<TradeOrderDO>()
                    .between(TradeOrderDO::getCreateTime, dayStart, dayEnd);

            int count = tradeOrderMapper.selectCount(wrapper);
            orderCountTrend.add(count);

            Long amount = tradeOrderMapper.selectSum(TradeOrderDO::getTotalPrice, wrapper);
            amountTrend.add(amount != null ? amount : 0L);

            Long paidAmount = tradeOrderMapper.selectSum(TradeOrderDO::getPayPrice,
                    wrapper.eq(TradeOrderDO::getPayStatus, true));
            paidAmountTrend.add(paidAmount != null ? paidAmount : 0L);

            currentDate = currentDate.plusDays(1);
        }

        trend.setDates(dates);
        trend.setOrderCountTrend(orderCountTrend);
        trend.setAmountTrend(amountTrend);
        trend.setPaidAmountTrend(paidAmountTrend);

        return trend;
    }

    private ConversionAnalysis analyzeConversion(TradeOrderAnalysisReqVO reqVO) {
        ConversionAnalysis conversion = new ConversionAnalysis();

        LambdaQueryWrapperX<TradeOrderDO> wrapper = new LambdaQueryWrapperX<TradeOrderDO>()
                .betweenIfPresent(TradeOrderDO::getCreateTime, reqVO.getStartTime(), reqVO.getEndTime());

        int createCount = tradeOrderMapper.selectCount(wrapper);
        conversion.setCreateCount(createCount);

        int paidCount = tradeOrderMapper.selectCount(wrapper.clone().eq(TradeOrderDO::getPayStatus, true));
        conversion.setPaidCount(paidCount);
        conversion.setPaidRate(createCount > 0 ? (double) paidCount / createCount * 100 : 0);

        int deliveryCount = tradeOrderMapper.selectCount(wrapper.clone().eq(TradeOrderDO::getStatus, TradeOrderStatusEnum.DELIVERY.getStatus()));
        conversion.setDeliveryCount(deliveryCount);
        conversion.setDeliveryRate(paidCount > 0 ? (double) deliveryCount / paidCount * 100 : 0);

        int receiveCount = tradeOrderMapper.selectCount(wrapper.clone().eq(TradeOrderDO::getStatus, TradeOrderStatusEnum.RECEIVE.getStatus()));
        conversion.setReceiveCount(receiveCount);
        conversion.setReceiveRate(deliveryCount > 0 ? (double) receiveCount / deliveryCount * 100 : 0);

        return conversion;
    }

    private AmountDistribution analyzeAmountDistribution(TradeOrderAnalysisReqVO reqVO) {
        AmountDistribution distribution = new AmountDistribution();

        LambdaQueryWrapperX<TradeOrderDO> wrapper = new LambdaQueryWrapperX<TradeOrderDO>()
                .betweenIfPresent(TradeOrderDO::getCreateTime, reqVO.getStartTime(), reqVO.getEndTime());

        distribution.setRange0_100(tradeOrderMapper.selectCount(wrapper.clone().between(TradeOrderDO::getPayPrice, 0, 10000)));
        distribution.setRange100_500(tradeOrderMapper.selectCount(wrapper.clone().between(TradeOrderDO::getPayPrice, 10001, 50000)));
        distribution.setRange500_1000(tradeOrderMapper.selectCount(wrapper.clone().between(TradeOrderDO::getPayPrice, 50001, 100000)));
        distribution.setRange1000_5000(tradeOrderMapper.selectCount(wrapper.clone().between(TradeOrderDO::getPayPrice, 100001, 500000)));
        distribution.setRange5000_plus(tradeOrderMapper.selectCount(wrapper.clone().gt(TradeOrderDO::getPayPrice, 500000)));

        return distribution;
    }

    private List<StatusDistribution> analyzeStatusDistribution(TradeOrderAnalysisReqVO reqVO) {
        List<StatusDistribution> distributions = new ArrayList<>();

        LambdaQueryWrapperX<TradeOrderDO> wrapper = new LambdaQueryWrapperX<TradeOrderDO>()
                .betweenIfPresent(TradeOrderDO::getCreateTime, reqVO.getStartTime(), reqVO.getEndTime());

        int totalCount = tradeOrderMapper.selectCount(wrapper);

        for (TradeOrderStatusEnum status : TradeOrderStatusEnum.values()) {
            int count = tradeOrderMapper.selectCount(wrapper.clone().eq(TradeOrderDO::getStatus, status.getStatus()));
            if (count > 0 || totalCount > 0) {
                StatusDistribution sd = new StatusDistribution();
                sd.setStatus(status.getStatus());
                sd.setStatusName(status.getDesc());
                sd.setCount(count);
                sd.setPercentage(totalCount > 0 ? (double) count / totalCount * 100 : 0);
                distributions.add(sd);
            }
        }

        return distributions;
    }

}
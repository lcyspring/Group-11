package com.meession.etm.module.trade.controller.admin.analysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "订单数据分析响应")
public class TradeOrderAnalysisRespVO {

    @Schema(description = "趋势分析")
    private TrendAnalysis trend;

    @Schema(description = "转化率分析")
    private ConversionAnalysis conversion;

    @Schema(description = "金额分布")
    private AmountDistribution amountDistribution;

    @Schema(description = "状态分布")
    private List<StatusDistribution> statusDistribution;

    @Data
    @Schema(description = "趋势分析")
    public static class TrendAnalysis {
        @Schema(description = "日期列表")
        private List<String> dates;
        @Schema(description = "订单数趋势")
        private List<Integer> orderCountTrend;
        @Schema(description = "金额趋势（分）")
        private List<Long> amountTrend;
        @Schema(description = "支付金额趋势（分）")
        private List<Long> paidAmountTrend;
    }

    @Data
    @Schema(description = "转化率分析")
    public static class ConversionAnalysis {
        @Schema(description = "订单创建数")
        private Integer createCount;
        @Schema(description = "支付订单数")
        private Integer paidCount;
        @Schema(description = "支付转化率")
        private Double paidRate;
        @Schema(description = "发货订单数")
        private Integer deliveryCount;
        @Schema(description = "发货转化率")
        private Double deliveryRate;
        @Schema(description = "完成订单数")
        private Integer receiveCount;
        @Schema(description = "完成转化率")
        private Double receiveRate;
    }

    @Data
    @Schema(description = "金额分布")
    public static class AmountDistribution {
        @Schema(description = "0-100元订单数")
        private Integer range0_100;
        @Schema(description = "100-500元订单数")
        private Integer range100_500;
        @Schema(description = "500-1000元订单数")
        private Integer range500_1000;
        @Schema(description = "1000-5000元订单数")
        private Integer range1000_5000;
        @Schema(description = "5000元以上订单数")
        private Integer range5000_plus;
    }

    @Data
    @Schema(description = "状态分布")
    public static class StatusDistribution {
        @Schema(description = "状态")
        private Integer status;
        @Schema(description = "状态名称")
        private String statusName;
        @Schema(description = "数量")
        private Integer count;
        @Schema(description = "占比")
        private Double percentage;
    }

}
package com.meession.etm.module.crm.controller.admin.customer.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CRM 客户星级评估 Response VO")
@Data
public class CrmCustomerStarAssessmentRespVO {

    @Schema(description = "客户编号", example = "1024")
    private Long id;

    @Schema(description = "客户名称", example = "腾讯科技")
    private String name;

    @Schema(description = "当前星级", example = "3")
    private Integer star;

    @Schema(description = "星级名称", example = "三星客户")
    private String starName;

    @Schema(description = "评估得分", example = "85")
    private Integer score;

    @Schema(description = "评估时间")
    private LocalDateTime assessmentTime;

    @Schema(description = "评估人", example = "张三")
    private String assessorName;

    @Schema(description = "评估备注", example = "客户潜力大，建议重点跟进")
    private String remark;

    @Schema(description = "评估维度详情")
    private AssessmentDimension dimension;

    @Data
    @Schema(description = "评估维度")
    public static class AssessmentDimension {

        @Schema(description = "成交金额得分", example = "25")
        private Integer dealAmountScore;

        @Schema(description = "成交次数得分", example = "20")
        private Integer dealCountScore;

        @Schema(description = "跟进频率得分", example = "20")
        private Integer followScore;

        @Schema(description = "客户等级得分", example = "15")
        private Integer levelScore;

        @Schema(description = "客户来源得分", example = "10")
        private Integer sourceScore;

        @Schema(description = "客户状态得分", example = "10")
        private Integer statusScore;

    }

}
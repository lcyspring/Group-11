package com.meession.etm.module.marketing.controller.admin.campaign.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 营销活动 Response VO")
@Data
@ToString(callSuper = true)
public class CampaignRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "活动名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "双十一促销活动")
    private String name;

    @Schema(description = "活动类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer type;

    @Schema(description = "活动状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "关联模板ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long templateId;

    @Schema(description = "目标类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer targetType;

    @Schema(description = "目标标签", example = "[\"VIP\"]")
    private String targetTags;

    @Schema(description = "指定用户ID列表", example = "[1,2,3]")
    private String targetUserIds;

    @Schema(description = "计划发送时间")
    private LocalDateTime sendTime;

    @Schema(description = "活动结束时间")
    private LocalDateTime endTime;

    @Schema(description = "BPM 流程实例ID")
    private String bpmProcessInstanceId;

    @Schema(description = "已发送数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer sentCount;

    @Schema(description = "成功数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "95")
    private Integer successCount;

    @Schema(description = "失败数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Integer failCount;

    @Schema(description = "备注", example = "备注内容")
    private String remark;

    @Schema(description = "创建人", example = "admin")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

}

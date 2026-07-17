package com.meession.etm.module.marketing.controller.admin.care.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 客户关怀模板配置 Response VO")
@Data
public class CustomerCareConfigRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "场景", example = "BIRTHDAY")
    private String scene;

    @Schema(description = "配置名称", example = "生日关怀")
    private String name;

    @Schema(description = "发送渠道", example = "SMS")
    private String channel;

    @Schema(description = "模板编码", example = "SMS_001")
    private String templateCode;

    @Schema(description = "模板参数模板", example = "{\"nickname\":\"{nickname}\"}")
    private String templateParamsTemplate;

    @Schema(description = "节日日期列表(JSON数组)", example = "[\"01-01\",\"10-01\"]")
    private String holidayDates;

    @Schema(description = "启用状态", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "用于客户生日自动祝福")
    private String remark;

    @Schema(description = "创建人", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}

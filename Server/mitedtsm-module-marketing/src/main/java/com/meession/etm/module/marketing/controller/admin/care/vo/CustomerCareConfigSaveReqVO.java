package com.meession.etm.module.marketing.controller.admin.care.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 客户关怀模板配置创建/修改 Request VO")
@Data
public class CustomerCareConfigSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "场景", requiredMode = Schema.RequiredMode.REQUIRED, example = "BIRTHDAY", allowableValues = {"BIRTHDAY", "HOLIDAY"})
    @NotEmpty(message = "场景不能为空")
    private String scene;

    @Schema(description = "配置名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "生日关怀")
    @NotEmpty(message = "配置名称不能为空")
    @Size(max = 64, message = "配置名称长度不能超过 64 个字符")
    private String name;

    @Schema(description = "发送渠道", requiredMode = Schema.RequiredMode.REQUIRED, example = "SMS", allowableValues = {"SMS", "MAIL"})
    @NotEmpty(message = "发送渠道不能为空")
    private String channel;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "SMS_001")
    @NotEmpty(message = "模板编码不能为空")
    @Size(max = 63, message = "模板编码长度不能超过 63 个字符")
    private String templateCode;

    @Schema(description = "模板参数模板", example = "{\"nickname\":\"{nickname}\"}")
    @Size(max = 512, message = "模板参数模板长度不能超过 512 个字符")
    private String templateParamsTemplate;

    @Schema(description = "节日日期列表(JSON数组，仅 HOLIDAY 场景)", example = "[\"01-01\",\"10-01\"]")
    @Size(max = 512, message = "节日日期列表长度不能超过 512 个字符")
    private String holidayDates;

    @Schema(description = "启用状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "启用状态不能为空")
    private Integer status;

    @Schema(description = "备注", example = "用于客户生日自动祝福")
    @Size(max = 256, message = "备注长度不能超过 256 个字符")
    private String remark;

}

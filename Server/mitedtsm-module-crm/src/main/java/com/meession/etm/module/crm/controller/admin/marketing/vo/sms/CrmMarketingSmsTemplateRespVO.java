package com.meession.etm.module.crm.controller.admin.marketing.vo.sms;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - CRM 营销短信模板 Response VO")
@Data
@ExcelIgnoreUnannotated
public class CrmMarketingSmsTemplateRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "促销通知模板")
    @ExcelProperty("模板名称")
    private String name;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROMOTION_NOTIFY")
    @ExcelProperty("模板编码")
    private String code;

    @Schema(description = "模板内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "尊敬的{name}，您有一份优惠待领取！")
    @ExcelProperty("模板内容")
    private String content;

    @Schema(description = "参数数组")
    private List<String> params;

    @Schema(description = "启用状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty("启用状态")
    private Integer status;

    @Schema(description = "关联的营销活动编号", example = "1024")
    private Long campaignId;

    @Schema(description = "短信渠道编号", example = "1")
    private Long channelId;

    @Schema(description = "短信渠道编码", example = "ALIYUN")
    @ExcelProperty("短信渠道编码")
    private String channelCode;

    @Schema(description = "短信 API 的模板编号", example = "SMS_123456789")
    @ExcelProperty("API模板编号")
    private String apiTemplateId;

    @Schema(description = "备注", example = "促销活动短信模板")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建者")
    @ExcelProperty("创建者")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("更新时间")
    private LocalDateTime updateTime;

}

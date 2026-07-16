package com.meession.etm.module.marketing.controller.admin.sms.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 营销短信模板分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SmsTemplateMarketingPageReqVO extends PageParam {

    @Schema(description = "开启状态", example = "0")
    private Integer status;

    @Schema(description = "模板编码，模糊匹配", example = "sms_marketing_01")
    private String code;

    @Schema(description = "模板内容，模糊匹配", example = "你好，{name}。双十一大促！")
    private String content;

}

package com.meession.etm.module.crm.controller.admin.marketing.vo;

import com.meession.etm.framework.common.enums.CommonStatusEnum;
import com.meession.etm.framework.common.validation.InEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrmCompetitorSaveReqVO {
    private Long id;
    @NotBlank(message = "竞争对手名称不能为空")
    @Size(max = 200, message = "竞争对手名称不能超过 200 个字符")
    private String name;
    @Size(max = 500, message = "网站地址不能超过 500 个字符")
    @Pattern(regexp = "^$|https?://.+", message = "网站地址必须使用 http 或 https")
    private String website;
    @Size(max = 2000, message = "优势不能超过 2000 个字符")
    private String strengths;
    @Size(max = 2000, message = "劣势不能超过 2000 个字符")
    private String weaknesses;
    @Size(max = 2000, message = "应对策略不能超过 2000 个字符")
    private String strategy;
    @NotNull(message = "负责人不能为空")
    private Long ownerUserId;
    @NotNull(message = "状态不能为空")
    @InEnum(CommonStatusEnum.class)
    private Integer status;
    @Size(max = 1000, message = "备注不能超过 1000 个字符")
    private String remark;
}

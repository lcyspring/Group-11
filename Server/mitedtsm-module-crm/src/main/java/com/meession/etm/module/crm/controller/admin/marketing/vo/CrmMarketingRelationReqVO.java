package com.meession.etm.module.crm.controller.admin.marketing.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CrmMarketingRelationReqVO {
    @NotNull(message = "关联对象类型不能为空")
    private Integer bizType;
    @NotNull(message = "关联对象编号不能为空")
    @Positive(message = "关联对象编号必须为正数")
    private Long bizId;
}

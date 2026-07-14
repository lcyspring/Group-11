package com.meession.etm.module.crm.controller.admin.customer.vo.poolrule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrmCustomerPoolRuleSaveReqVO {

    private Long id;

    @NotBlank(message = "规则名称不能为空")
    private String name;

    @NotNull(message = "规则类型不能为空")
    private Integer ruleType;

    @NotNull(message = "执行类型不能为空")
    private Integer executeType;

    private String cronExpression;

    private Boolean enabled;

    private Integer sort;

    private String remark;

    private String config;

}

package com.meession.etm.module.crm.controller.admin.reimbursement.vo;

import com.meession.etm.framework.common.enums.CommonStatusEnum;
import com.meession.etm.framework.common.validation.InEnum;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CrmExpenseCategorySaveReqVO {
    private Long id;

    @NotBlank(message = "分类编码不能为空")
    @Pattern(regexp = "[A-Z][A-Z0-9_-]{1,39}", message = "分类编码必须以大写字母开头且只能包含大写字母、数字、下划线和连字符")
    private String code;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称不能超过 100 个字符")
    private String name;

    @NotNull(message = "分类状态不能为空")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

    @NotNull(message = "显示顺序不能为空")
    @Min(value = 0, message = "显示顺序不能小于 0")
    @Max(value = 9999, message = "显示顺序不能超过 9999")
    private Integer sort;

    @Size(max = 500, message = "分类说明不能超过 500 个字符")
    private String description;
}

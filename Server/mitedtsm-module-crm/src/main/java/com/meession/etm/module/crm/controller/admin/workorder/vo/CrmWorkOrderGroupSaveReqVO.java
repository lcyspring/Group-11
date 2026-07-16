package com.meession.etm.module.crm.controller.admin.workorder.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CrmWorkOrderGroupSaveReqVO {

    private Long id;
    @NotBlank
    @Pattern(regexp = "^[a-z][a-z0-9_-]{1,31}$", message = "处理组编码必须以小写字母开头，只能包含小写字母、数字、下划线和连字符")
    private String code;
    @NotBlank
    @Size(max = 100)
    private String name;
    @NotNull
    private Long managerUserId;
    @NotEmpty
    @Size(max = 4)
    private List<Integer> supportedTypes;
    @NotEmpty
    @Size(max = 100)
    private List<Long> memberUserIds;
    @NotNull
    private Integer status;
    @NotNull
    private Integer sort;
    @Size(max = 500)
    private String remark;
}

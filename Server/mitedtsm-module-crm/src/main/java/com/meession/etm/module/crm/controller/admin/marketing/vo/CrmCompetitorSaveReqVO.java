package com.meession.etm.module.crm.controller.admin.marketing.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrmCompetitorSaveReqVO {
    private Long id;
    @NotBlank(message = "竞争对手名称不能为空")
    private String name;
    private String website;
    private String strengths;
    private String weaknesses;
    private String strategy;
    @NotNull(message = "负责人不能为空")
    private Long ownerUserId;
    private Integer status;
    private String remark;
}

package com.meession.etm.module.crm.controller.admin.marketing.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmCompetitorRespVO {
    private Long id;
    private String name;
    private String website;
    private String strengths;
    private String weaknesses;
    private String strategy;
    private Long ownerUserId;
    private String ownerUserName;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

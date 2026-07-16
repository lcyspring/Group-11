package com.meession.etm.module.crm.controller.admin.workorder.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CrmWorkOrderGroupRespVO {
    private Long id;
    private String code;
    private String name;
    private Long managerUserId;
    private String managerUserName;
    private List<Integer> supportedTypes;
    private List<Long> memberUserIds;
    private List<String> memberUserNames;
    private Integer status;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

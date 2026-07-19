package com.meession.etm.module.crm.controller.admin.workorder.vo;

import lombok.Data;

@Data
public class CrmWorkOrderSlaPolicyRespVO {
    private Long id;
    private String code;
    private String name;
    private Integer priority;
    private Integer responseMinutes;
    private Integer resolutionMinutes;
    private Integer escalationMinutes;
    private Boolean enabled;
    private Integer sort;
}

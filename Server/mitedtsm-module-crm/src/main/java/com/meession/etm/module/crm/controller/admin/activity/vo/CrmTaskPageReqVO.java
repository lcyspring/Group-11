package com.meession.etm.module.crm.controller.admin.activity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmTaskPageReqVO extends CrmActivityPageReqVO {
    private Integer type;
    private Integer status;
    private Long assigneeUserId;
}

package com.meession.etm.module.crm.controller.admin.activity.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmActivityPageReqVO extends PageParam {
    @NotNull(message = "CRM 对象类型不能为空")
    private Integer bizType;
    @NotNull(message = "CRM 对象编号不能为空")
    private Long bizId;
}

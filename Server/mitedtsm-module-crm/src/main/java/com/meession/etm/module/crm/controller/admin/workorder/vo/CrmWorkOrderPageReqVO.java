package com.meession.etm.module.crm.controller.admin.workorder.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - CRM 客服工单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmWorkOrderPageReqVO extends PageParam {
    private String no;
    private String title;
    private Integer type;
    private Integer priority;
    private Integer status;
    private Long customerId;
    private Long handlerUserId;
    /** 1 我创建的、2 我处理的；为空时查询两者并集。 */
    @Min(value = 1, message = "工单视图类型必须是 1 或 2")
    @Max(value = 2, message = "工单视图类型必须是 1 或 2")
    private Integer sceneType;
    /** 待办口径：我处理的待处理/处理中，或我创建的已退回。 */
    private Boolean backlog;
}

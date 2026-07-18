package com.meession.etm.module.crm.controller.admin.workorder.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

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
    /** 导出时可指定勾选的工单；为空时按其他筛选条件查询。 */
    private Set<Long> ids;
    /** 1 我创建的、2 我处理的、3 抄送我的、4 处理组未分配；为空时查询授权并集。 */
    @Min(value = 1, message = "工单视图类型必须在 1 到 4 之间")
    @Max(value = 4, message = "工单视图类型必须在 1 到 4 之间")
    private Integer sceneType;
    /** 待办口径：我处理的待处理/处理中，或我创建的已退回。 */
    private Boolean backlog;
}

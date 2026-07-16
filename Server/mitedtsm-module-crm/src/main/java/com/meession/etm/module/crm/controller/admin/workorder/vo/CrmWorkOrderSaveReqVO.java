package com.meession.etm.module.crm.controller.admin.workorder.vo;

import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.module.crm.enums.workorder.CrmWorkOrderPriorityEnum;
import com.meession.etm.module.crm.enums.workorder.CrmWorkOrderSourceTypeEnum;
import com.meession.etm.module.crm.enums.workorder.CrmWorkOrderTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - CRM 客服工单保存 Request VO")
@Data
public class CrmWorkOrderSaveReqVO {

    private Long id;

    @NotBlank(message = "工单标题不能为空")
    @Size(max = 200, message = "工单标题不能超过 200 个字符")
    private String title;

    @NotNull(message = "工单类型不能为空")
    @InEnum(CrmWorkOrderTypeEnum.class)
    private Integer type;

    @NotNull(message = "优先级不能为空")
    @InEnum(CrmWorkOrderPriorityEnum.class)
    private Integer priority;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotNull(message = "来源类型不能为空")
    @InEnum(CrmWorkOrderSourceTypeEnum.class)
    private Integer sourceType;

    private Long sourceId;

    private Long handlerUserId;

    private Long groupId;

    @Size(max = 100, message = "抄送人不能超过 100 个")
    private List<Long> ccUserIds;

    @NotBlank(message = "工单描述不能为空")
    @Size(max = 5000, message = "工单描述不能超过 5000 个字符")
    private String description;

    @Size(max = 10, message = "工单附件不能超过 10 个")
    private List<@Size(max = 1024, message = "附件地址不能超过 1024 个字符") String> attachmentUrls;
}

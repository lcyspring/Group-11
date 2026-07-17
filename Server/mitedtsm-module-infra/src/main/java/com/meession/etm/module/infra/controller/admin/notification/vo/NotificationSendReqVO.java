package com.meession.etm.module.infra.controller.admin.notification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 消息通知批量发送 Request VO")
@Data
public class NotificationSendReqVO {

    @Schema(description = "通知类型，参见 NotificationTypeEnum 枚举", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "通知类型不能为空")
    private Integer type;

    @Schema(description = "通知标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "系统通知")
    @NotBlank(message = "通知标题不能为空")
    @Size(max = 200, message = "通知标题长度不能超过 200 个字符")
    private String title;

    @Schema(description = "通知内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "通知内容")
    @NotBlank(message = "通知内容不能为空")
    private String content;

    @Schema(description = "接收人用户编号列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1024, 2048]")
    @NotEmpty(message = "接收人用户编号列表不能为空")
    private List<Long> receiverUserIds;

}

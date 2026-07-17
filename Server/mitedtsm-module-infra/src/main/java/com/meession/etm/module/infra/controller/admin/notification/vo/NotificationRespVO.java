package com.meession.etm.module.infra.controller.admin.notification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 消息通知信息 Response VO")
@Data
public class NotificationRespVO {

    @Schema(description = "通知主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "通知类型，参见 NotificationTypeEnum 枚举", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer type;

    @Schema(description = "通知标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "系统通知")
    private String title;

    @Schema(description = "通知内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "通知内容")
    private String content;

    @Schema(description = "接收人用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long receiverUserId;

    @Schema(description = "发送人用户编号", example = "1024")
    private Long senderUserId;

    @Schema(description = "是否已读", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean readStatus;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "时间戳格式")
    private LocalDateTime createTime;

}

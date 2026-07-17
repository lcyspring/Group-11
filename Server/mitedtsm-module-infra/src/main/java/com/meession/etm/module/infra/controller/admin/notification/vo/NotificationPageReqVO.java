package com.meession.etm.module.infra.controller.admin.notification.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 消息通知分页 Request VO")
@Data
public class NotificationPageReqVO extends PageParam {

    @Schema(description = "通知类型，参见 NotificationTypeEnum 枚举", example = "1")
    private Integer type;

    @Schema(description = "是否已读", example = "false")
    private Boolean readStatus;

    @Schema(description = "通知标题，模糊匹配", example = "系统通知")
    private String title;

}

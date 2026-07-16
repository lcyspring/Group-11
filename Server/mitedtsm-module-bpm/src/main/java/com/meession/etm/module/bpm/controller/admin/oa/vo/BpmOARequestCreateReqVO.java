package com.meession.etm.module.bpm.controller.admin.oa.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - OA请示审批创建 Request VO")
@Data
public class BpmOARequestCreateReqVO {

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "关于采购设备的请示")
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "因业务需要，申请采购新设备")
    @NotBlank(message = "内容不能为空")
    private String content;

    @Schema(description = "请示类型", example = "purchase")
    private String type;

    @Schema(description = "附件", example = "[\"file1.pdf\",\"file2.doc\"]")
    private String attachments;

    @Schema(description = "发起人自选审批人 Map", example = "{taskKey1: [1, 2]}")
    private Map<String, List<Long>> startUserSelectAssignees;

}
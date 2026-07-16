package com.meession.etm.module.bpm.controller.admin.oa.vo;

import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOARequestDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - OA请示审批 Response VO")
@Data
public class BpmOARequestRespVO {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "标题", example = "关于采购设备的请示")
    private String title;

    @Schema(description = "内容", example = "因业务需要，申请采购新设备")
    private String content;

    @Schema(description = "请示类型", example = "purchase")
    private String type;

    @Schema(description = "附件", example = "[\"file1.pdf\"]")
    private String attachments;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "流程实例ID", example = "123456")
    private String processInstanceId;

    @Schema(description = "创建时间", example = "2026-07-16 10:00:00")
    private LocalDateTime createTime;

    public static BpmOARequestRespVO build(BpmOARequestDO request) {
        return BeanUtils.toBean(request, BpmOARequestRespVO.class);
    }

}
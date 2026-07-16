package com.meession.etm.module.bpm.controller.admin.oa.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - OA请示审批分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmOARequestPageReqVO extends PageParam {

    @Schema(description = "标题", example = "采购")
    private String title;

    @Schema(description = "请示类型", example = "purchase")
    private String type;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime[] createTime;

}
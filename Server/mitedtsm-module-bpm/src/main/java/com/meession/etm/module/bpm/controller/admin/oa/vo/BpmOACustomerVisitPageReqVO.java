package com.meession.etm.module.bpm.controller.admin.oa.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - OA客户拜访申请分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmOACustomerVisitPageReqVO extends PageParam {

    @Schema(description = "客户名称", example = "华为")
    private String customerName;

    @Schema(description = "联系人", example = "张三")
    private String contactPerson;

    @Schema(description = "拜访目的", example = "产品演示")
    private String purpose;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime[] createTime;

}
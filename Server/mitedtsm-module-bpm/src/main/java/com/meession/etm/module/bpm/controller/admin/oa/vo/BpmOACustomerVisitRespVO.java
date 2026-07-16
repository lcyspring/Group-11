package com.meession.etm.module.bpm.controller.admin.oa.vo;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOACustomerVisitDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - OA客户拜访申请 Response VO")
@Data
public class BpmOACustomerVisitRespVO {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "客户ID", example = "1")
    private Long customerId;

    @Schema(description = "客户名称", example = "华为技术有限公司")
    private String customerName;

    @Schema(description = "联系人", example = "张三")
    private String contactPerson;

    @Schema(description = "联系电话", example = "13800138000")
    private String contactPhone;

    @Schema(description = "拜访地址", example = "深圳市南山区科技园")
    private String visitAddress;

    @Schema(description = "拜访时间", example = "2026-07-16 14:00:00")
    private LocalDateTime visitTime;

    @Schema(description = "拜访目的", example = "产品演示")
    private String purpose;

    @Schema(description = "拜访内容", example = "向客户演示新产品功能")
    private String content;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "流程实例ID", example = "123456")
    private String processInstanceId;

    @Schema(description = "创建时间", example = "2026-07-16 10:00:00")
    private LocalDateTime createTime;

    public static BpmOACustomerVisitRespVO build(BpmOACustomerVisitDO visit) {
        return BeanUtils.toBean(visit, BpmOACustomerVisitRespVO.class);
    }

}
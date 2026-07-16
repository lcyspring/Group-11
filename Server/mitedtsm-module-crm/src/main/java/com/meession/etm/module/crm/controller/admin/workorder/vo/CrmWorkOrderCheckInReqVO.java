package com.meession.etm.module.crm.controller.admin.workorder.vo;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CrmWorkOrderCheckInReqVO {
    @NotNull(message = "工单编号不能为空")
    private Long id;
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal latitude;
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal longitude;
    @Positive(message = "定位精度必须大于 0")
    private BigDecimal accuracyMeters;
    private String remark;
}

package com.meession.etm.module.crm.controller.admin.marketing.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CrmCustomerBirthdayPageReqVO extends PageParam {
    private String keyword;
    @Min(1)
    @Max(366)
    @NotNull
    private Integer upcomingDays = 30;
}

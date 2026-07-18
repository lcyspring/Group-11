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
    /** 1 客户自身，2 联系人；默认 2 保持旧 API 兼容。 */
    @Min(1)
    @Max(2)
    private Integer targetType = 2;
    private String keyword;
    @Min(1)
    @Max(366)
    @NotNull
    private Integer upcomingDays = 30;
}

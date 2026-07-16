package com.meession.etm.module.crm.controller.admin.marketing.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data @EqualsAndHashCode(callSuper = true)
public class CrmCustomerCareRecordPageReqVO extends PageParam {
    private Long planId; private Integer status; private LocalDate eventDate;
}

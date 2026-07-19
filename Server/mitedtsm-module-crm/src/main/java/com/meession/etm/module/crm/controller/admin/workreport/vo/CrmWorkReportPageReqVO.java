package com.meession.etm.module.crm.controller.admin.workreport.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class CrmWorkReportPageReqVO extends PageParam {
    private Integer reportType;
    private Integer status;
    private LocalDate[] reportDate;
    private Boolean received;
}

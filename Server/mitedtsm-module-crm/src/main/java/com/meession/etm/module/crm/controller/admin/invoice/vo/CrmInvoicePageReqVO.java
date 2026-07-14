package com.meession.etm.module.crm.controller.admin.invoice.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.module.crm.enums.common.CrmSceneTypeEnum;
import com.meession.etm.module.crm.enums.invoice.CrmInvoiceDirectionEnum;
import com.meession.etm.module.crm.enums.invoice.CrmInvoiceStatusEnum;
import com.meession.etm.module.crm.enums.invoice.CrmInvoiceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.meession.etm.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmInvoicePageReqVO extends PageParam {

    private String no;
    private String invoiceNo;
    private Long contractId;
    private Long customerId;
    @InEnum(CrmInvoiceStatusEnum.class)
    private Integer status;
    @InEnum(CrmInvoiceTypeEnum.class)
    private Integer type;
    @InEnum(CrmInvoiceDirectionEnum.class)
    private Integer direction;
    @InEnum(CrmSceneTypeEnum.class)
    private Integer sceneType;
    @Schema(description = "开票日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] invoiceDate;
}

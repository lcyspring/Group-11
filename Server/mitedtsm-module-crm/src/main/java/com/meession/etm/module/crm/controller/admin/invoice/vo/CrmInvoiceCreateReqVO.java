package com.meession.etm.module.crm.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - CRM 发票草稿创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmInvoiceCreateReqVO extends CrmInvoiceBaseReqVO {

    @NotNull(message = "合同编号不能为空")
    private Long contractId;
}

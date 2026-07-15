package com.meession.etm.module.crm.controller.admin.reimbursement.vo;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CrmReimbursementItemSaveReqVO {
    @NotNull(message = "费用分类不能为空")
    private Long categoryId;

    @NotNull(message = "费用发生日期不能为空")
    private LocalDate occurredDate;

    @NotNull(message = "费用金额不能为空")
    @DecimalMin(value = "0.000001", message = "费用金额必须大于 0")
    @Digits(integer = 18, fraction = 6, message = "费用金额最多 18 位整数和 6 位小数")
    private BigDecimal amount;

    @NotBlank(message = "费用说明不能为空")
    @Size(max = 500, message = "费用说明不能超过 500 个字符")
    private String description;

    @Size(max = 100, message = "票据号码不能超过 100 个字符")
    private String invoiceNo;

    @Size(max = 10, message = "每条费用明细最多关联 10 个附件")
    private List<@Size(max = 500, message = "附件地址不能超过 500 个字符") String> attachmentUrls;
}

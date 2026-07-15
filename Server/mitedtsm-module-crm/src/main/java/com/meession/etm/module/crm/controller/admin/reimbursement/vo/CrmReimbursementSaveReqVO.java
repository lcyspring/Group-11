package com.meession.etm.module.crm.controller.admin.reimbursement.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CrmReimbursementSaveReqVO {
    private Long id;
    private Long customerId;
    private Long contractId;

    @NotNull(message = "费用开始日期不能为空")
    private LocalDate expenseStartDate;

    @NotNull(message = "费用结束日期不能为空")
    private LocalDate expenseEndDate;

    @NotBlank(message = "报销事由不能为空")
    @Size(min = 5, max = 500, message = "报销事由长度必须为 5 到 500 个字符")
    private String reason;

    @Size(max = 1000, message = "备注不能超过 1000 个字符")
    private String remark;

    @NotEmpty(message = "报销单至少需要一条费用明细")
    @Size(max = 100, message = "单张报销单最多包含 100 条费用明细")
    @Valid
    private List<CrmReimbursementItemSaveReqVO> items;
}

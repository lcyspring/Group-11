package com.meession.etm.module.crm.controller.admin.customer.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 客户导入预检确认 Request VO")
@Data
public class CrmCustomerImportConfirmReqVO {

    @NotNull(message = "预检任务编号不能为空")
    private Long id;
}

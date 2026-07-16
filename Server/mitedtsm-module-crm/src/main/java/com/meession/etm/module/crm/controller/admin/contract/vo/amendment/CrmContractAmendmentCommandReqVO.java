package com.meession.etm.module.crm.controller.admin.contract.vo.amendment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrmContractAmendmentCommandReqVO {
    @NotNull(message = "合同编号不能为空")
    private Long contractId;
    @NotNull(message = "补充协议编号不能为空")
    private Long id;
}

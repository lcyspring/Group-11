package com.meession.etm.module.crm.controller.admin.contract.vo.amendment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CrmContractAmendmentSaveReqVO {

    private Long id;

    @NotNull(message = "合同编号不能为空")
    private Long contractId;

    @NotBlank(message = "客户端请求号不能为空")
    @Size(max = 128, message = "客户端请求号长度不能超过 128")
    private String clientRequestId;

    @NotBlank(message = "补充协议标题不能为空")
    @Size(max = 200, message = "补充协议标题长度不能超过 200")
    private String title;

    @NotBlank(message = "变更原因不能为空")
    @Size(max = 1000, message = "变更原因长度不能超过 1000")
    private String reason;

    @NotBlank(message = "合同名称不能为空")
    @Size(max = 255, message = "合同名称长度不能超过 255")
    private String contractName;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @NotNull(message = "整单折扣不能为空")
    @DecimalMin(value = "0", message = "整单折扣不能小于 0")
    @DecimalMax(value = "100", message = "整单折扣不能大于 100")
    private BigDecimal discountPercent;

    private Long signContactId;
    private Long signUserId;

    @Size(max = 2000, message = "备注长度不能超过 2000")
    private String remark;

    @Valid
    private List<Product> products;

    @Data
    public static class Product {
        private Long id;
        @NotNull(message = "产品编号不能为空")
        private Long productId;
        @NotNull(message = "合同价格不能为空")
        @Positive(message = "合同价格必须大于 0")
        private BigDecimal contractPrice;
        @NotNull(message = "产品数量不能为空")
        @Positive(message = "产品数量必须大于 0")
        private BigDecimal count;
    }
}

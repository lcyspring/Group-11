package com.meession.etm.module.crm.controller.admin.contract.vo.amendment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CrmContractAmendmentRespVO {
    private Long id;
    private Long contractId;
    private String no;
    private String clientRequestId;
    private Integer baseVersion;
    private Integer targetVersion;
    private String title;
    private String reason;
    private Integer auditStatus;
    private String processInstanceId;
    private BigDecimal amountBefore;
    private BigDecimal amountAfter;
    private BigDecimal amountDelta;
    private Long submitterUserId;
    private LocalDateTime submitTime;
    private LocalDateTime effectiveTime;
    private LocalDateTime createTime;
    private String contractName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal discountPercent;
    private Long signContactId;
    private Long signUserId;
    private String remark;
    private List<Product> products;

    @Data
    public static class Product {
        private Long id;
        private Long productId;
        private String productNameSnapshot;
        private String productNoSnapshot;
        private Integer productUnitSnapshot;
        private BigDecimal productPrice;
        private BigDecimal contractPrice;
        private BigDecimal count;
        private BigDecimal totalPrice;
    }
}

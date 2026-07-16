package com.meession.etm.module.crm.controller.admin.fulfillment.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CrmContractFulfillmentRespVO {

    private boolean enabled;
    private String policyVersion;
    private String currencyMode;
    private String erpCurrencyCode;
    private List<String> allowedSourceCurrencies;
    private boolean eligible;
    private boolean sourceInvalidated;
    private List<String> blockers = new ArrayList<>();
    private Long contractId;
    private Integer contractVersion;
    private Long crmCustomerId;
    private String crmCustomerName;
    private Long erpCustomerId;
    private String erpCustomerName;
    private List<ProductMapping> productMappings = new ArrayList<>();
    private FulfillmentRecord record;

    @Data
    public static class ProductMapping {
        private Long crmProductId;
        private String crmProductName;
        private String crmProductNo;
        private Long erpProductId;
        private String erpProductName;
        private boolean mapped;
    }

    @Data
    public static class FulfillmentRecord {
        private Long id;
        private Integer status;
        private String requestId;
        private String requestHash;
        private Integer attemptCount;
        private Long erpOrderId;
        private String erpOrderNo;
        private Integer erpOrderStatus;
        private String sourceCurrencyCode;
        private String erpCurrencyCode;
        private BigDecimal exchangeRate;
        private BigDecimal sourceGrossAmount;
        private BigDecimal erpTotalAmount;
        private BigDecimal totalCount;
        private BigDecimal outCount;
        private BigDecimal returnCount;
        private String lastErrorCode;
        private String lastErrorMessage;
        private LocalDateTime lastAttemptTime;
        private LocalDateTime completedTime;
        private LocalDateTime lastSyncTime;
    }
}

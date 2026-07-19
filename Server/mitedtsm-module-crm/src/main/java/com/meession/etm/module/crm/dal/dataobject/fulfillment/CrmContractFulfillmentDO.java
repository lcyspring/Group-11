package com.meession.etm.module.crm.dal.dataobject.fulfillment;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("crm_contract_fulfillment")
@KeySequence("crm_contract_fulfillment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmContractFulfillmentDO extends BaseDO {

    @TableId
    private Long id;
    private Long contractId;
    private Integer contractVersion;
    private String requestId;
    private String requestHash;
    private String requestSnapshot;
    private Integer status;
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
    private Integer attemptCount;
    private String lastErrorCode;
    private String lastErrorMessage;
    private LocalDateTime lastAttemptTime;
    private LocalDateTime completedTime;
    private LocalDateTime lastSyncTime;
}

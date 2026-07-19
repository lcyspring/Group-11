package com.meession.etm.module.crm.dal.dataobject.receivable;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CrmReceivableWriteOffAmountDO {
    private Long receivableId;
    private BigDecimal amount;
}

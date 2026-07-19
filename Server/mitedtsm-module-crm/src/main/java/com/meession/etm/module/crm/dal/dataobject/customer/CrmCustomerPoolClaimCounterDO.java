package com.meession.etm.module.crm.dal.dataobject.customer;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/** Concurrency-safe daily self-claim quota counter. */
@TableName("crm_customer_pool_claim_counter")
@KeySequence("crm_customer_pool_claim_counter_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmCustomerPoolClaimCounterDO extends BaseDO {

    @TableId
    private Long id;
    private Long userId;
    private LocalDate claimDate;
    private Integer claimCount;
}

package com.meession.etm.module.crm.dal.dataobject.customer;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.meession.etm.module.crm.enums.customer.CrmCustomerPoolReceiveLimitTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName(value = "crm_customer_pool_receive_limit")
@KeySequence("crm_customer_pool_receive_limit_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmCustomerPoolReceiveLimitDO extends BaseDO {

    @TableId
    private Long id;

    private Long userId;

    private Integer limitType;

    private Integer maxCount;

    private Integer usedCount;

    private LocalDateTime periodStartTime;

    private LocalDateTime periodEndTime;

}

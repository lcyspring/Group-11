package com.meession.etm.module.crm.dal.dataobject.customer;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.meession.etm.module.crm.enums.customer.CrmCustomerPoolReceiveSourceTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName(value = "crm_customer_pool_receive")
@KeySequence("crm_customer_pool_receive_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmCustomerPoolReceiveDO extends BaseDO {

    @TableId
    private Long id;

    private Long customerId;

    private Long receiveUserId;

    private LocalDateTime receiveTime;

    private LocalDateTime freezeEndTime;

    private Integer sourceType;

    private String remark;

}

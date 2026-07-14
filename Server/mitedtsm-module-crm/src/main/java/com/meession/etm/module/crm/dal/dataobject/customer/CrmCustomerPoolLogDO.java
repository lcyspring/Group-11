package com.meession.etm.module.crm.dal.dataobject.customer;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.meession.etm.module.crm.enums.customer.CrmCustomerPoolOperationTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName(value = "crm_customer_pool_log")
@KeySequence("crm_customer_pool_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmCustomerPoolLogDO extends BaseDO {

    @TableId
    private Long id;

    private Long customerId;

    private String customerName;

    private Integer operationType;

    private Long operationUserId;

    private String operationUserName;

    private Long beforeOwnerUserId;

    private String beforeOwnerUserName;

    private Long afterOwnerUserId;

    private String afterOwnerUserName;

    private String reason;

    private Long ruleId;

}

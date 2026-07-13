package com.meession.etm.module.crm.dal.dataobject.customer;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * CRM 客户公海归属变更记录。
 */
@TableName("crm_customer_owner_record")
@KeySequence("crm_customer_owner_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmCustomerOwnerRecordDO extends BaseDO {

    @TableId
    private Long id;

    /** 客户编号。 */
    private Long customerId;

    /** 放入公海前或领取后的负责人编号。 */
    private Long ownerUserId;

    /** 归属变更类型，见 CrmCustomerOwnerRecordTypeEnum。 */
    private Integer type;

}

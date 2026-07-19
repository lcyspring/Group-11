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
 * CRM 客户归属变更记录。
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

    /**
     * 兼容原公海统计的负责人编号。
     *
     * 进入公海时为原负责人，其他类型为新负责人。
     */
    private Long ownerUserId;

    /** 变更前负责人编号。 */
    private Long previousOwnerUserId;

    /** 变更后负责人编号。 */
    private Long newOwnerUserId;

    /** 归属变更类型，见 CrmCustomerOwnerRecordTypeEnum。 */
    private Integer type;

    /** Machine-readable source of this immutable ownership event. */
    private String source;

    /** Optional human-readable reason captured with this event. */
    private String reason;

}

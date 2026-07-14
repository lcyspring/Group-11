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

import java.time.LocalDateTime;

/** CRM 客户生命周期不可变变更记录。 */
@TableName("crm_customer_lifecycle_record")
@KeySequence("crm_customer_lifecycle_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmCustomerLifecycleRecordDO extends BaseDO {

    @TableId
    private Long id;
    private Long customerId;
    private Integer fromStatus;
    private Integer toStatus;
    private String reason;
    private Long operatorUserId;
    private LocalDateTime changeTime;

}

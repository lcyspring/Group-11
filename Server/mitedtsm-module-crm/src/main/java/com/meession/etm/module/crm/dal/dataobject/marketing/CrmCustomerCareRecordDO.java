package com.meession.etm.module.crm.dal.dataobject.marketing;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("crm_customer_care_record")
@KeySequence("crm_customer_care_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmCustomerCareRecordDO extends BaseDO {
    @TableId private Long id;
    private Long planId;
    private Long customerId;
    private Long contactId;
    private LocalDate eventDate;
    private Integer channel;
    private Integer status;
    private String failureReason;
    private Long providerLogId;
    private LocalDateTime sentAt;
}

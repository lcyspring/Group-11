package com.meession.etm.module.crm.dal.dataobject.refund;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("crm_receivable_refund")
@KeySequence("crm_receivable_refund_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmReceivableRefundDO extends BaseDO {

    @TableId
    private Long id;
    private String no;
    private Long receivableId;
    private Long customerId;
    private Long contractId;
    private Long ownerUserId;
    private Integer type;
    private LocalDateTime refundTime;
    private BigDecimal amount;
    private String reason;
    private String remark;
    private String processInstanceId;
    private Integer auditStatus;
}

package com.meession.etm.module.crm.dal.dataobject.invoice;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

import java.time.LocalDateTime;

@TableName("crm_invoice_action_record")
@KeySequence("crm_invoice_action_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmInvoiceActionRecordDO extends BaseDO {

    @TableId
    private Long id;
    private Long invoiceId;
    private Integer actionType;
    private Integer fromStatus;
    private Integer toStatus;
    private Long operatorUserId;
    private LocalDateTime actionTime;
    private String providerRequestId;
    private String remark;
}

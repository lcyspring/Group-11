package com.meession.etm.module.crm.dal.dataobject.invoice;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("crm_invoice")
@KeySequence("crm_invoice_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmInvoiceDO extends BaseDO {

    @TableId
    private Long id;
    private String no;
    private Long contractId;
    private Long customerId;
    private Long ownerUserId;
    private Long handlerUserId;
    private Integer direction;
    private Long originalInvoiceId;
    private Integer status;
    private Integer type;
    private BigDecimal amount;
    private BigDecimal redAmount;
    private String invoiceNo;
    private LocalDateTime invoiceDate;

    // 购方开票信息快照
    private String title;
    private String taxNo;
    private String registeredAddress;
    private String registeredPhone;
    private String bankName;
    private String bankAccount;
    private String email;
    private String content;

    private String externalProvider;
    private String externalRequestId;
    private String externalInvoiceId;
    private String issueRemark;
    private String remark;
}

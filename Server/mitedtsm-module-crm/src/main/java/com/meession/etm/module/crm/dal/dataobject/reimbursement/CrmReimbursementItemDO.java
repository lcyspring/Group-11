package com.meession.etm.module.crm.dal.dataobject.reimbursement;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("crm_reimbursement_item")
@KeySequence("crm_reimbursement_item_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmReimbursementItemDO extends BaseDO {
    @TableId
    private Long id;
    private Long reimbursementId;
    private Long categoryId;
    private LocalDate occurredDate;
    private BigDecimal amount;
    private String description;
    private String invoiceNo;
    private String attachmentUrls;
    private Integer sort;
}

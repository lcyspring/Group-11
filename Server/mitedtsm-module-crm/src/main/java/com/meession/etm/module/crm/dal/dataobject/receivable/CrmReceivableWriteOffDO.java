package com.meession.etm.module.crm.dal.dataobject.receivable;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("crm_receivable_write_off") @KeySequence("crm_receivable_write_off_seq")
@Data @EqualsAndHashCode(callSuper = true) @Builder @NoArgsConstructor @AllArgsConstructor
public class CrmReceivableWriteOffDO extends BaseDO {
    @TableId private Long id; private Long receivableId; private BigDecimal amount;
    private LocalDateTime writeOffTime; private Integer sourceType; private String referenceNo;
    private String remark; private Integer status; private LocalDateTime reversedAt;
}

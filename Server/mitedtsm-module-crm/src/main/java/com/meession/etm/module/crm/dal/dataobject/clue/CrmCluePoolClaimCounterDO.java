package com.meession.etm.module.crm.dal.dataobject.clue;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

@TableName("crm_clue_pool_claim_counter")
@KeySequence("crm_clue_pool_claim_counter_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmCluePoolClaimCounterDO extends BaseDO {

    @TableId
    private Long id;
    private Long userId;
    private LocalDate claimDate;
    private Integer claimCount;
}

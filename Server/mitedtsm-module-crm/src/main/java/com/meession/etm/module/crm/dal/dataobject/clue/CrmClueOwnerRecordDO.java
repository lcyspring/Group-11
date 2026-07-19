package com.meession.etm.module.crm.dal.dataobject.clue;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName("crm_clue_owner_record")
@KeySequence("crm_clue_owner_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmClueOwnerRecordDO extends BaseDO {

    @TableId
    private Long id;
    private Long clueId;
    private Long previousOwnerUserId;
    private Long newOwnerUserId;
    private Integer type;
    private String source;
    private String reason;
}

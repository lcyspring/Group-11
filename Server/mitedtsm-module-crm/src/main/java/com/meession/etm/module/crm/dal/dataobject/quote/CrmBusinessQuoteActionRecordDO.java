package com.meession.etm.module.crm.dal.dataobject.quote;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("crm_business_quote_action_record")
@KeySequence("crm_business_quote_action_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmBusinessQuoteActionRecordDO extends BaseDO {
    @TableId
    private Long id;
    private Long quoteId;
    private Integer actionType;
    private Integer fromStatus;
    private Integer toStatus;
    private Long operatorUserId;
    private String remark;
}

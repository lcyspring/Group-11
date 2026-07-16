package com.meession.etm.module.crm.dal.dataobject.marketing;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("crm_marketing_consent")
@KeySequence("crm_marketing_consent_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmMarketingConsentDO extends BaseDO {
    @TableId private Long id;
    private Long customerId;
    private Long contactId;
    private Integer channel;
    private Integer status;
    private String source;
    private LocalDateTime occurredAt;
}

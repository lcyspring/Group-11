package com.meession.etm.module.crm.dal.dataobject.marketing;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("crm_marketing_link_recipient")
@KeySequence("crm_marketing_link_recipient_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmMarketingLinkRecipientDO extends BaseDO {
    @TableId private Long id;
    private Long linkId;
    private Long recipientId;
    private String trackingToken;
    private LocalDateTime firstClickedAt;
    private LocalDateTime lastClickedAt;
    private Integer clickCount;
}

package com.meession.etm.module.crm.dal.dataobject.marketing;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("crm_marketing_link")
@KeySequence("crm_marketing_link_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmMarketingLinkDO extends BaseDO {
    @TableId private Long id;
    private Long broadcastId;
    private String code;
    private String name;
    private String targetUrl;
}

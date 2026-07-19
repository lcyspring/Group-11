package com.meession.etm.module.crm.dal.dataobject.marketing;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName("crm_competitor")
@KeySequence("crm_competitor_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmCompetitorDO extends BaseDO {
    @TableId
    private Long id;
    private String name;
    private String website;
    private String strengths;
    private String weaknesses;
    private String strategy;
    private Long ownerUserId;
    private Integer status;
    private String remark;
}

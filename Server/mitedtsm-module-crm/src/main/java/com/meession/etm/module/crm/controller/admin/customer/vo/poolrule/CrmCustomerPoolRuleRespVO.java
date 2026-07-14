package com.meession.etm.module.crm.controller.admin.customer.vo.poolrule;

import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolRuleDO;
import com.meession.etm.framework.common.util.object.BeanUtils;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmCustomerPoolRuleRespVO {

    private Long id;

    private String name;

    private Integer ruleType;

    private String ruleTypeName;

    private Integer executeType;

    private String executeTypeName;

    private String cronExpression;

    private Boolean enabled;

    private Integer sort;

    private String remark;

    private String config;

    private String creator;

    private LocalDateTime createTime;

    private String updater;

    private LocalDateTime updateTime;

    public static CrmCustomerPoolRuleRespVO of(CrmCustomerPoolRuleDO rule) {
        return BeanUtils.toBean(rule, CrmCustomerPoolRuleRespVO.class);
    }

}

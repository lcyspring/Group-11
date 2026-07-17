package com.meession.etm.module.crm.dal.dataobject.customer;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName(value = "crm_customer_config")
@KeySequence("crm_customer_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmCustomerConfigDO extends BaseDO {

    @TableId
    private Long id;

    private String configType;

    private Integer configValue;

    private String configName;

    private String color;

    private Integer sort;

    private String remark;

    private Boolean status;

}
package com.meession.etm.module.crm.dal.dataobject.fulfillment;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("crm_erp_product_mapping")
@KeySequence("crm_erp_product_mapping_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmErpProductMappingDO extends BaseDO {

    @TableId
    private Long id;
    private Long crmProductId;
    private Long erpProductId;
    private String remark;
}

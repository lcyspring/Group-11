package com.meession.etm.module.crm.dal.dataobject.reimbursement;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

@TableName("crm_expense_category")
@KeySequence("crm_expense_category_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmExpenseCategoryDO extends BaseDO {
    @TableId
    private Long id;
    private String code;
    private String name;
    private Integer status;
    private Integer sort;
    private String description;
}

package com.meession.etm.module.crm.dal.dataobject.workorder;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@TableName(value = "crm_work_order_group", autoResultMap = true)
@KeySequence("crm_work_order_group_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmWorkOrderGroupDO extends BaseDO {

    @TableId
    private Long id;
    private String code;
    private String name;
    private Long managerUserId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Integer> supportedTypes;
    private Integer status;
    private Integer sort;
    private String remark;
}

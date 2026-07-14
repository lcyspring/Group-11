package com.meession.etm.module.workorder.dal.dataobject;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 工单类型 DO
 *
 * @author fwx
 */
@TableName("wo_work_order_type")
@KeySequence("wo_work_order_type_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderTypeDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 类型名称
     */
    private String name;

    /**
     * 类型编码
     */
    private String code;

    /**
     * 类型描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态: 0-启用, 1-禁用
     */
    private Integer status;

}

package com.meession.etm.module.workorder.dal.dataobject;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 工单 DO
 *
 * @author fwx
 */
@TableName("wo_work_order")
@KeySequence("wo_work_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderDO extends BaseDO {

    /**
     * 工单编号
     */
    @TableId
    private Long id;

    /**
     * 工单标题
     */
    private String title;

    /**
     * 工单内容/描述
     */
    private String content;

    /**
     * 工单类型编号
     *
     * 关联 {@link WorkOrderTypeDO#getId()}
     */
    private Long typeId;

    /**
     * 优先级: 0-低, 1-中, 2-高, 3-紧急
     *
     * 枚举 {@link com.meession.etm.module.workorder.enums.WorkOrderPriorityEnum}
     */
    private Integer priority;

    /**
     * 工单状态: 0-待处理, 1-处理中, 2-已完成, 3-已关闭, 4-已退回
     *
     * 枚举 {@link com.meession.etm.module.workorder.enums.WorkOrderStatusEnum}
     */
    private Integer status;

    /**
     * 处理人用户编号
     *
     * 关联 AdminUserDO 的 id 字段
     */
    private Long handlerUserId;

    /**
     * 发起人用户编号
     *
     * 关联 AdminUserDO 的 id 字段
     */
    private Long submitterUserId;

    /**
     * 处理结果/备注
     */
    private String result;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    /**
     * 预计完成时间
     */
    private LocalDateTime expectedFinishTime;

    /**
     * 实际完成时间
     */
    private LocalDateTime finishTime;

    /**
     * 关联客户编号(可选)
     */
    private Long customerId;

    /**
     * 关联商机编号(可选)
     */
    private Long businessId;

    /**
     * 备注
     */
    private String remark;

}

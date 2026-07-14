package com.meession.etm.module.workorder.enums;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 工单状态枚举
 *
 * @author fwx
 */
@RequiredArgsConstructor
@Getter
public enum WorkOrderStatusEnum implements ArrayValuable<Integer> {

    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    COMPLETED(2, "已完成"),
    CLOSED(3, "已关闭"),
    RETURNED(4, "已退回");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(WorkOrderStatusEnum::getStatus).toArray(Integer[]::new);

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static WorkOrderStatusEnum fromStatus(Integer status) {
        return Arrays.stream(values())
                .filter(value -> value.getStatus().equals(status))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断状态是否允许流转到目标状态
     *
     * @param currentStatus 当前状态
     * @param targetStatus  目标状态
     * @return 是否允许流转
     */
    public static boolean canTransition(Integer currentStatus, Integer targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        if (currentStatus.equals(targetStatus)) {
            return false;
        }
        // 已关闭和已完成状态不允许再变更
        if (CLOSED.getStatus().equals(currentStatus) || COMPLETED.getStatus().equals(currentStatus)) {
            return false;
        }
        return true;
    }

}

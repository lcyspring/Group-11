package com.meession.etm.module.workorder.enums;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 工单优先级枚举
 *
 * @author fwx
 */
@RequiredArgsConstructor
@Getter
public enum WorkOrderPriorityEnum implements ArrayValuable<Integer> {

    LOW(0, "低"),
    MEDIUM(1, "中"),
    HIGH(2, "高"),
    URGENT(3, "紧急");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(WorkOrderPriorityEnum::getPriority).toArray(Integer[]::new);

    /**
     * 优先级值
     */
    private final Integer priority;
    /**
     * 优先级名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static WorkOrderPriorityEnum fromPriority(Integer priority) {
        return Arrays.stream(values())
                .filter(value -> value.getPriority().equals(priority))
                .findFirst()
                .orElse(null);
    }

}

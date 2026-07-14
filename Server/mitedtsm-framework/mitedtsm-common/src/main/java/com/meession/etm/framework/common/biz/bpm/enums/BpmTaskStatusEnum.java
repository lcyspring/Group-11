package com.meession.etm.framework.common.biz.bpm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * BPM 任务状态通用枚举
 * 
 * 用于表示流程任务的执行状态
 * 
 * @author jxq
 */
@Getter
@AllArgsConstructor
public enum BpmTaskStatusEnum {

    /**
     * 待处理
     */
    WAIT(0, "待处理"),

    /**
     * 进行中
     */
    RUNNING(1, "进行中"),

    /**
     * 审批通过
     */
    APPROVE(2, "审批通过"),

    /**
     * 审批不通过
     */
    REJECT(3, "审批不通过"),

    /**
     * 已取消
     */
    CANCEL(4, "已取消"),

    /**
     * 已退回
     */
    RETURN(5, "已退回"),

    /**
     * 已委派
     */
    DELEGATE(6, "已委派");

    /**
     * 状态值
     */
    private final Integer status;

    /**
     * 状态名称
     */
    private final String name;

    /**
     * 判断是否为终态（审批通过、不通过、取消）
     */
    public static boolean isTerminal(Integer status) {
        return APPROVE.getStatus().equals(status) 
                || REJECT.getStatus().equals(status) 
                || CANCEL.getStatus().equals(status);
    }

    /**
     * 判断是否为进行中
     */
    public static boolean isRunning(Integer status) {
        return RUNNING.getStatus().equals(status);
    }

    /**
     * 判断是否为待处理
     */
    public static boolean isWait(Integer status) {
        return WAIT.getStatus().equals(status);
    }

}

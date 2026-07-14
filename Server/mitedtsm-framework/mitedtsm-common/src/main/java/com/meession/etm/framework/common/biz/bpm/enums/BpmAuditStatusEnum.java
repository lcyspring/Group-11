package com.meession.etm.framework.common.biz.bpm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * BPM 审批状态通用枚举
 * 
 * 用于业务实体的审批状态管理，提供统一的审批状态定义
 * 
 * @author jxq
 */
@Getter
@AllArgsConstructor
public enum BpmAuditStatusEnum {

    /**
     * 草稿
     */
    DRAFT(0, "草稿"),

    /**
     * 审批中
     */
    PROCESS(1, "审批中"),

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
    CANCEL(4, "已取消");

    /**
     * 状态值
     */
    private final Integer status;

    /**
     * 状态名称
     */
    private final String name;

    /**
     * 判断是否为审批中状态
     */
    public static boolean isProcess(Integer status) {
        return PROCESS.getStatus().equals(status);
    }

    /**
     * 判断是否为审批通过状态
     */
    public static boolean isApprove(Integer status) {
        return APPROVE.getStatus().equals(status);
    }

    /**
     * 判断是否为审批拒绝状态
     */
    public static boolean isReject(Integer status) {
        return REJECT.getStatus().equals(status);
    }

    /**
     * 判断是否为已取消状态
     */
    public static boolean isCancel(Integer status) {
        return CANCEL.getStatus().equals(status);
    }

    /**
     * 判断是否为草稿状态
     */
    public static boolean isDraft(Integer status) {
        return DRAFT.getStatus().equals(status);
    }

}

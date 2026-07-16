package com.meession.etm.module.marketing.enums;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 营销活动状态枚举
 *
 * @author MITEDTSM
 */
@Getter
@AllArgsConstructor
public enum CampaignStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "草稿"),
    PENDING_APPROVAL(1, "待审核"),
    APPROVED(2, "已审核"),
    IN_PROGRESS(3, "进行中"),
    FINISHED(4, "已结束"),
    CANCELLED(5, "已终止");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CampaignStatusEnum::getStatus).toArray(Integer[]::new);

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static boolean isDraft(Integer status) {
        return DRAFT.getStatus().equals(status);
    }

    public static boolean isPendingApproval(Integer status) {
        return PENDING_APPROVAL.getStatus().equals(status);
    }

    public static boolean isApproved(Integer status) {
        return APPROVED.getStatus().equals(status);
    }

    public static boolean isInProgress(Integer status) {
        return IN_PROGRESS.getStatus().equals(status);
    }

}

package com.meession.etm.module.marketing.enums;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 营销活动目标类型枚举
 *
 * @author MITEDTSM
 */
@Getter
@AllArgsConstructor
public enum CampaignTargetTypeEnum implements ArrayValuable<Integer> {

    ALL_MEMBERS(1, "全部会员"),
    SPECIFIC_USERS(2, "指定会员"),
    BY_TAGS(3, "按标签筛选");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CampaignTargetTypeEnum::getType).toArray(Integer[]::new);

    /**
     * 类型
     */
    private final Integer type;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}

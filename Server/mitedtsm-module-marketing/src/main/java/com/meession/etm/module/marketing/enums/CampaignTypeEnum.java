package com.meession.etm.module.marketing.enums;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 营销活动类型枚举
 *
 * @author MITEDTSM
 */
@Getter
@AllArgsConstructor
public enum CampaignTypeEnum implements ArrayValuable<Integer> {

    SMS(1, "短信营销"),
    MAIL(2, "邮件营销");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CampaignTypeEnum::getType).toArray(Integer[]::new);

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

package com.meession.etm.module.crm.enums.marketing;

import cn.hutool.core.util.ArrayUtil;
import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 营销活动类型枚举
 *
 * @author mitedtsm
 */
@Getter
@AllArgsConstructor
public enum CrmMarketingCampaignTypeEnum implements ArrayValuable<Integer> {

    SMS(1, "短信营销"),
    MAIL(2, "邮件营销");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmMarketingCampaignTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static CrmMarketingCampaignTypeEnum fromType(Integer type) {
        return ArrayUtil.firstMatch(e -> e.getType().equals(type), values());
    }

}

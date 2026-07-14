package com.meession.etm.module.crm.enums.marketing;

import cn.hutool.core.util.ArrayUtil;
import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 营销活动状态枚举
 *
 * @author mitedtsm
 */
@Getter
@AllArgsConstructor
public enum CrmMarketingCampaignStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "草稿"),
    RUNNING(1, "进行中"),
    FINISHED(2, "已结束"),
    CANCELLED(3, "已取消");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmMarketingCampaignStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static CrmMarketingCampaignStatusEnum fromStatus(Integer status) {
        return ArrayUtil.firstMatch(e -> e.getStatus().equals(status), values());
    }

}

package com.meession.etm.module.marketing.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 客户关怀场景枚举
 * <p>
 * 注意：节日场景暂仅支持公历固定日期（如元旦 1月1日、国庆 10月1日）。
 * 农历节日（春节、中秋等）每年公历日期不同，需产品方在配置中手动填写当年的公历日期。
 *
 * @author MITEDTSM
 */
@Getter
@AllArgsConstructor
public enum CareSceneEnum {

    BIRTHDAY("BIRTHDAY", "生日关怀"),
    HOLIDAY("HOLIDAY", "节日关怀");

    private final String scene;
    private final String name;

}

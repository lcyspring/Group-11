package com.meession.etm.module.infra.enums.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 参数配置分类枚举
 */
@Getter
@AllArgsConstructor
public enum ConfigCategoryEnum {

    SYSTEM(1, "系统配置"),
    EMAIL(2, "邮件配置"),
    SMS(3, "短信配置"),
    BPM(4, "审批配置"),
    SECURITY(5, "安全配置"),
    NOTIFICATION(6, "通知配置");

    /**
     * 分类编号
     */
    private final Integer category;
    /**
     * 分类名称
     */
    private final String categoryName;

    /**
     * 根据分类编号获得枚举
     *
     * @param category 分类编号
     * @return 分类枚举
     */
    public static ConfigCategoryEnum valueOf(Integer category) {
        if (category == null) {
            return null;
        }
        for (ConfigCategoryEnum value : values()) {
            if (value.category.equals(category)) {
                return value;
            }
        }
        return null;
    }

}

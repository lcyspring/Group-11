package com.meession.etm.framework.security.core.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * 用于控制用户对数据的可见范围
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /** 数据权限类型 */
    DataScopeType scopeType() default DataScopeType.ALL;

    /** 用户表的别名 */
    String userAlias() default "u";

    /** 部门表的别名 */
    String deptAlias() default "d";

    /** 数据权限类型枚举 */
    enum DataScopeType {
        /** 全部数据权限 */
        ALL,
        /** 本部门数据权限 */
        DEPT_CUSTOM,
        /** 本部门及以下数据权限 */
        DEPT_AND_CHILD,
        /** 仅本人数据权限 */
        SELF
    }
}

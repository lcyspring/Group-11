package com.meession.etm.framework.web.core.annotation;

import java.lang.annotation.*;

/**
 * API 版本注解
 * 用于标识 Controller 或方法的 API 版本号
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiVersion {

    /** API 版本号，例如 "v1", "v2" */
    String value() default "v1";
}

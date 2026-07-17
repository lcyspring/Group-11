package com.meession.etm.framework.security.core.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解
 * 基于 Redis 实现的分布式限流
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {

    /** 限流 key 前缀 */
    String key() default "";

    /** 限流时间窗口 */
    int time() default 60;

    /** 限流时间窗口单位 */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /** 在限流时间窗口内允许的最大访问次数 */
    int count() default 100;

    /** 限流类型 */
    LimitType limitType() default LimitType.DEFAULT;

    /** 限流类型枚举 */
    enum LimitType {
        /** 全局限流 */
        DEFAULT,
        /** 根据 IP 限流 */
        IP
    }
}

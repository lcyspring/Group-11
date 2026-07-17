package com.meession.etm.framework.web.core.annotation;

import java.lang.annotation.*;

/**
 * 响应包装注解
 * 标注在 Controller 方法上，自动将返回值包装为 CommonResult
 * 如果返回值已经是 CommonResult，则不做二次包装
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseWrapper {
}

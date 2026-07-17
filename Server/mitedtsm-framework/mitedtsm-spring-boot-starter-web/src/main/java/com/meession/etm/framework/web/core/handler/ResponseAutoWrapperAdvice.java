package com.meession.etm.framework.web.core.handler;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.web.core.annotation.ResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应自动包装 Advice
 * 对标注了 @ResponseWrapper 的 Controller，自动将返回值包装为 CommonResult
 */
@RestControllerAdvice
@Slf4j
public class ResponseAutoWrapperAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 检查方法或其所在类是否有 @ResponseWrapper 注解
        return returnType.hasMethodAnnotation(ResponseWrapper.class)
                || returnType.getContainingClass() != null
                && returnType.getDeclaringClass().isAnnotationPresent(ResponseWrapper.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // 如果已经是 CommonResult，不重复包装
        if (body instanceof CommonResult) {
            return body;
        }
        return CommonResult.success(body);
    }
}

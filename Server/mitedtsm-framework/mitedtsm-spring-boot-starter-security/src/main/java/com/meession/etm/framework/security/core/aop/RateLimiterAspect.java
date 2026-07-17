package com.meession.etm.framework.security.core.aop;

import cn.hutool.core.util.StrUtil;
import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.meession.etm.framework.security.core.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Collections;

/**
 * 限流切面
 * 基于 Redis Lua 脚本实现滑动窗口限流
 */
@Aspect
@Component
@Slf4j
public class RateLimiterAspect {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private HttpServletRequest httpServletRequest;

    /** Lua 脚本：原子性限流检查 */
    private static final String LUA_SCRIPT =
            "local key = KEYS[1] " +
            "local limit = tonumber(ARGV[1]) " +
            "local window = tonumber(ARGV[2]) " +
            "local current = tonumber(redis.call('get', key) or '0') " +
            "if current >= limit then " +
            "  return 0 " +
            "else " +
            "  redis.call('incr', key) " +
            "  if current == 0 then " +
            "    redis.call('expire', key, window) " +
            "  end " +
            "  return 1 " +
            "end";

    private static final DefaultRedisScript<Long> REDIS_SCRIPT;

    static {
        REDIS_SCRIPT = new DefaultRedisScript<>();
        REDIS_SCRIPT.setScriptText(LUA_SCRIPT);
        REDIS_SCRIPT.setResultType(Long.class);
    }

    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimiter rateLimiter) {
        String combineKey = getCombineKey(rateLimiter, point);
        int windowSeconds = (int) rateLimiter.timeUnit().toSeconds(rateLimiter.time());

        try {
            Long result = stringRedisTemplate.execute(REDIS_SCRIPT,
                    Collections.singletonList(combineKey),
                    String.valueOf(rateLimiter.count()),
                    String.valueOf(windowSeconds));

            if (result == null || result == 0L) {
                log.warn("[doBefore][接口({}) 访问过于频繁，已被限流]", combineKey);
                throw new ServiceException(GlobalErrorCodeConstants.TOO_MANY_REQUESTS.getCode(),
                        "访问过于频繁，请稍后再试");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[doBefore][限流异常]", e);
            // 限流异常时放行，避免影响正常业务
        }
    }

    private String getCombineKey(RateLimiter rateLimiter, JoinPoint point) {
        StringBuilder sb = new StringBuilder("rate_limit:");
        switch (rateLimiter.limitType()) {
            case IP:
                sb.append(getClientIp()).append(":");
                break;
            case DEFAULT:
            default:
                break;
        }
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        sb.append(method.getDeclaringClass().getName()).append(".").append(method.getName());
        if (StrUtil.isNotEmpty(rateLimiter.key())) {
            sb.append(":").append(rateLimiter.key());
        }
        return sb.toString();
    }

    private String getClientIp() {
        String ip = httpServletRequest.getHeader("X-Forwarded-For");
        if (StrUtil.isNotEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        return httpServletRequest.getRemoteAddr();
    }
}

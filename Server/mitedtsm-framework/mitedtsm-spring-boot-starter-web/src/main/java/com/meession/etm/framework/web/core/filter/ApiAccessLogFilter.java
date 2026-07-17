package com.meession.etm.framework.web.core.filter;

import cn.hutool.core.util.StrUtil;
import com.meession.etm.framework.common.util.json.JsonUtils;
import com.meession.etm.framework.common.util.servlet.ServletUtils;
import com.meession.etm.framework.web.core.util.WebFrameworkUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * API 访问日志过滤器
 * 记录每次 API 请求的详细信息，包括请求参数、响应状态、耗时等
 */
@Slf4j
@Order(-200)
public class ApiAccessLogFilter extends OncePerRequestFilter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            Long userId = WebFrameworkUtils.getLoginUserId(request);

            log.info("[doFilterInternal][{} {} | 状态:{} | 耗时:{}ms | IP:{} | userId:{} | UA:{}]",
                    method, requestUri, status, duration, clientIp,
                    userId != null ? userId : "anonymous",
                    StrUtil.maxLength(userAgent, 100));
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 排除静态资源和 Swagger 文档
        String uri = request.getRequestURI();
        return StrUtil.startWithAny(uri, "/swagger-ui", "/v3/api-docs", "/favicon.ico")
                || StrUtil.endWithAny(uri, ".js", ".css", ".ico", ".png", ".jpg", ".gif");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StrUtil.isNotEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}

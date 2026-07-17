package com.meession.etm.framework.security.core.filter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.util.json.JsonUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * IP 黑白名单过滤器
 * 支持动态配置 IP 黑白名单，实现访问控制
 */
@Slf4j
@Order(-100)
public class IpsFilter implements Filter {

    /** 黑名单 IP 集合 */
    private final Set<String> blacklistIps = new CopyOnWriteArraySet<>();
    /** 白名单 IP 集合 */
    private final Set<String> whitelistIps = new CopyOnWriteArraySet<>();
    /** 是否启用黑名单模式（默认 true） */
    private volatile boolean blacklistEnabled = true;
    /** 是否启用白名单模式 */
    private volatile boolean whitelistEnabled = false;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = getClientIp(httpRequest);

        // 白名单模式：不在白名单中的 IP 直接拒绝
        if (whitelistEnabled && CollUtil.isNotEmpty(whitelistIps)) {
            if (!whitelistIps.contains(clientIp)) {
                log.warn("[doFilter][IP({}) 不在白名单中，拒绝访问]", clientIp);
                writeRejectResponse(httpResponse, "IP 访问被拒绝：不在白名单中");
                return;
            }
        }

        // 黑名单模式：在黑名单中的 IP 直接拒绝
        if (blacklistEnabled && blacklistIps.contains(clientIp)) {
            log.warn("[doFilter][IP({}) 在黑名单中，拒绝访问]", clientIp);
            writeRejectResponse(httpResponse, "IP 访问被拒绝：在黑名单中");
            return;
        }

        chain.doFilter(request, response);
    }

    /** 添加黑名单 IP */
    public void addBlacklistIp(String ip) {
        blacklistIps.add(ip);
        log.info("[addBlacklistIp][添加黑名单 IP: {}]", ip);
    }

    /** 移除黑名单 IP */
    public void removeBlacklistIp(String ip) {
        blacklistIps.remove(ip);
    }

    /** 添加白名单 IP */
    public void addWhitelistIp(String ip) {
        whitelistIps.add(ip);
    }

    /** 移除白名单 IP */
    public void removeWhitelistIp(String ip) {
        whitelistIps.remove(ip);
    }

    /** 获取黑名单列表 */
    public Set<String> getBlacklistIps() {
        return Collections.unmodifiableSet((Set<String>) blacklistIps);
    }

    /** 获取白名单列表 */
    public Set<String> getWhitelistIps() {
        return Collections.unmodifiableSet((Set<String>) whitelistIps);
    }

    public void setBlacklistEnabled(boolean enabled) {
        this.blacklistEnabled = enabled;
    }

    public void setWhitelistEnabled(boolean enabled) {
        this.whitelistEnabled = enabled;
    }

    public boolean isBlacklistEnabled() {
        return blacklistEnabled;
    }

    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
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

    private void writeRejectResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(403);
        response.getWriter().write(JsonUtils.toJsonString(
                CommonResult.error(403, message)));
    }
}

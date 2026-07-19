package com.meession.etm.framework.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;
import java.util.Collections;
import java.util.List;

@ConfigurationProperties(prefix = "mitedtsm.security")
@Validated
@Data
public class SecurityProperties {

    /**
     * HTTP 请求时，访问令牌的请求 Header
     */
    @NotEmpty(message = "Token Header 不能为空")
    private String tokenHeader = "Authorization";
    /**
     * HTTP 请求时，访问令牌的请求参数
     *
     * 初始目的：解决 WebSocket 无法通过 header 传参，只能通过 token 参数拼接
     */
    @NotEmpty(message = "Token Parameter 不能为空")
    private String tokenParameter = "token";

    /**
     * mock 模式的开关
     */
    @NotNull(message = "mock 模式的开关不能为空")
    private Boolean mockEnable = false;
    /**
     * mock 模式的密钥
     * 一定要配置密钥，保证安全性
     */
    private String mockSecret;

    @AssertTrue(message = "启用 mock 模式时必须显式配置密钥")
    public boolean isMockSecretValid() {
        return !Boolean.TRUE.equals(mockEnable) || mockSecret != null && !mockSecret.isBlank();
    }

    /**
     * 免登录的 URL 列表
     */
    private List<String> permitAllUrls = Collections.emptyList();

    /**
     * PasswordEncoder 加密复杂度，越高开销越大
     */
    @Min(value = 10, message = "BCrypt 强度不能低于 10")
    @Max(value = 16, message = "BCrypt 强度不能高于 16")
    private Integer passwordEncoderLength = 10;
}

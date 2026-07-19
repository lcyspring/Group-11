package com.meession.etm.framework.web.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "mitedtsm.web")
@Validated
@Data
public class WebProperties {

    @NotNull(message = "APP API 不能为空")
    private Api appApi = new Api("/app-api", "**.controller.app.**");
    @NotNull(message = "Admin API 不能为空")
    private Api adminApi = new Api("/admin-api", "**.controller.admin.**");

    @NotNull(message = "Admin UI 不能为空")
    private Ui adminUi;
    @NotNull(message = "CORS 配置不能为空")
    @Valid
    private Cors cors = new Cors();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Valid
    public static class Api {

        /**
         * API 前缀，实现所有 Controller 提供的 RESTFul API 的统一前缀
         *
         *
         * 意义：通过该前缀，避免 Swagger、Actuator 意外通过 Nginx 暴露出来给外部，带来安全性问题
         *      这样，Nginx 只需要配置转发到 /api/* 的所有接口即可。
         *
         * @see MitedtsmWebAutoConfiguration#configurePathMatch(PathMatchConfigurer)
         */
        @NotEmpty(message = "API 前缀不能为空")
        private String prefix;

        /**
         * Controller 所在包的 Ant 路径规则
         *
         * 主要目的是，给该 Controller 设置指定的 {@link #prefix}
         */
        @NotEmpty(message = "Controller 所在包不能为空")
        private String controller;

    }

    @Data
    @Valid
    public static class Ui {

        /**
         * 访问地址
         */
        private String url;

    }

    @Data
    public static class Cors {

        /** 允许携带凭证的精确来源或 Spring Origin Pattern。空集合代表禁止跨域。 */
        @NotNull(message = "CORS 允许来源不能为空")
        private List<String> allowedOriginPatterns = new ArrayList<>();
        @NotEmpty(message = "CORS 允许请求头不能为空")
        private List<String> allowedHeaders = List.of("Authorization", "Content-Type", "tenant-id", "Accept-Language");
        @NotEmpty(message = "CORS 允许方法不能为空")
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        @NotNull(message = "CORS 凭证开关不能为空")
        private Boolean allowCredentials = true;
        @NotNull(message = "CORS 缓存时间不能为空")
        private Duration maxAge = Duration.ofHours(1);

    }

}

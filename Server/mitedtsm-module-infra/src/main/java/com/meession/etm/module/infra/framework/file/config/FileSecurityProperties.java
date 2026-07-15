package com.meession.etm.module.infra.framework.file.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
@Data
@Validated
@ConfigurationProperties(prefix = "mitedtsm.infra.file-security")
public class FileSecurityProperties {

    /** Paths that may only be read through an authorized business endpoint. */
    @NotEmpty
    private List<@NotBlank String> protectedPathPrefixes = new ArrayList<>();

    public boolean isProtectedPath(String path) {
        String normalizedPath = normalizePath(path);
        return protectedPathPrefixes.stream()
                .map(FileSecurityProperties::normalizePath)
                .anyMatch(prefix -> normalizedPath.equals(prefix) || normalizedPath.startsWith(prefix + "/"));
    }

    private static String normalizePath(String path) {
        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        normalized = Path.of(normalized).normalize().toString().replace('\\', '/');
        while (normalized.startsWith("../")) {
            normalized = normalized.substring(3);
        }
        return normalized;
    }
}

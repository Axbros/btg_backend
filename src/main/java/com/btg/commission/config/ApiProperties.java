package com.btg.commission.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 对外 HTTP API 统一前缀，与 {@code application.yml} 中 {@code btg.api.base-path} 一致。
 */
@Data
@Component
@ConfigurationProperties(prefix = "btg.api")
public class ApiProperties {

    /**
     * 例如 {@code /api/v1}，不含末尾斜杠。
     */
    private String basePath = "/api/v1";

    public String getBasePath() {
        if (!StringUtils.hasText(basePath)) {
            return "/api/v1";
        }
        return basePath.replaceAll("/+$", "");
    }
}

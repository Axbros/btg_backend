package com.btg.commission.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Data
@Component
@ConfigurationProperties(prefix = "btg.file")
public class FileStorageProperties {

    /**
     * 本地存储根目录（绝对路径或相对应用工作目录）。
     */
    private String uploadDir = "./uploads";

    /**
     * 对外访问基址，不含末尾斜杠；由配置 {@code btg.file.public-base-url} 注入（如 application.yml）。
     */
    private String publicBaseUrl;

    /**
     * 单文件最大字节，默认 10MB
     */
    private long maxFileSizeBytes = 10 * 1024 * 1024;

    public Path uploadRoot() {
        return Path.of(uploadDir).toAbsolutePath().normalize();
    }
}

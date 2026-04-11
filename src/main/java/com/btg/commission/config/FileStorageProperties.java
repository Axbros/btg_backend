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
     * 对外访问基址，不含末尾斜杠，例如 http://localhost:8080 或 https://api.example.com
     */
    private String publicBaseUrl = "http://localhost:8080";

    /**
     * 单文件最大字节，默认 10MB
     */
    private long maxFileSizeBytes = 10 * 1024 * 1024;

    public Path uploadRoot() {
        return Path.of(uploadDir).toAbsolutePath().normalize();
    }
}

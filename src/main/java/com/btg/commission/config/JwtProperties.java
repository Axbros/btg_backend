package com.btg.commission.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "btg.jwt")
public class JwtProperties {

    /**
     * HS256 secret; use a long random string in production.
     */
    private String secret = "change-me";

    private long expirationMs = 86_400_000L;
}

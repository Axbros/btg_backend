package com.btg.commission.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Data
@Component
@ConfigurationProperties(prefix = "bitget")
public class BitgetProperties {

    /** Bitget REST 根地址，无末尾斜杠 */
    private String baseUrl = "https://api.bitget.com";

    public String getBaseUrl() {
        if (!StringUtils.hasText(baseUrl)) {
            return "https://api.bitget.com";
        }
        return baseUrl.replaceAll("/+$", "");
    }
}

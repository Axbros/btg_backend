package com.btg.commission.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "btg.dashboard")
public class DashboardProperties {

    /**
     * pending-summary 是否改读 reminder 聚合；默认 false（保持旧口径）
     */
    private boolean pendingSummaryReadFromReminder = false;
}

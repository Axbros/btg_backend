package com.btg.commission.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReminderReconcileItemVO {
    String metricKey;
    Integer legacyCount;
    Integer reminderCount;
    Integer diffCount;
}

package com.btg.commission.vo;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class ReminderReconcileResultVO {
    Long userId;
    Boolean hasDiff;
    LocalDateTime comparedAt;
    List<ReminderReconcileItemVO> items;
}

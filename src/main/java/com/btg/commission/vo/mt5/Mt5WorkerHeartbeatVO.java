package com.btg.commission.vo.mt5;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mt5WorkerHeartbeatVO {

    private String workerCode;
    private LocalDateTime lastHeartbeatTime;
    private Integer status;
    private Boolean online;
    private Integer currentAccountsSynced;
}

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
public class Mt5WorkerVO {

    private Long id;
    private String workerCode;
    private String workerName;
    private Integer status;
    private Integer maxAccounts;
    private Integer currentAccounts;
    private LocalDateTime lastHeartbeatTime;
    private Integer heartbeatExpireSeconds;
    private String version;
    private String hostName;
    private String ipAddress;
    private String remark;
    private Boolean online;
}

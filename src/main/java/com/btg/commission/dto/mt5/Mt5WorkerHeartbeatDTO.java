package com.btg.commission.dto.mt5;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class Mt5WorkerHeartbeatDTO {

    @NotBlank(message = "workerCode 不能为空")
    @Size(max = 100)
    private String workerCode;

    @Size(max = 50)
    private String version;

    @Size(max = 255)
    private String hostName;

    @Size(max = 100)
    private String ipAddress;

    /** Worker 侧上报的当前处理账号数（可选，服务端仍以库内统计为准） */
    private Integer currentAccounts;

    @Size(max = 500)
    private String remark;
}

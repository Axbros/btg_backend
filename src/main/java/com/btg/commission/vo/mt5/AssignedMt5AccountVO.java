package com.btg.commission.vo.mt5;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedMt5AccountVO {

    private Long userId;
    private String realName;
    private String mobile;
    private String serverName;
    private String tradingAccountId;
    private String tradingAccountPassword;
    private String exchangeUid;
    /** {@code btg_mt5_worker.id} */
    private Long assignedWorkerId;
    private String assignedWorkerCode;
}

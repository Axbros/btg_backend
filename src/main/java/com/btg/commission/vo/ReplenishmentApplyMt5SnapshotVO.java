package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class ReplenishmentApplyMt5SnapshotVO {

    @Schema(description = "提交补仓时快照中的 MT5 账户号")
    String accountId;

    @Schema(description = "提交补仓时快照中的服务器名")
    String serverName;

    BigDecimal balance;
    BigDecimal equity;

    @Schema(description = "提交补仓时快照时间")
    LocalDateTime snapshotTime;
}

package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class Mt5SnapshotVO {

    @Schema(description = "快照主键")
    private Long id;
    @Schema(description = "系统用户 ID，未关联则为 null")
    private Long userId;
    @Schema(description = "MT5 账户号")
    private String accountId;
    @Schema(description = "服务器名")
    private String serverName;
    @Schema(description = "当前余额")
    private BigDecimal balance;
    @Schema(description = "当前净值")
    private BigDecimal equity;
    @Schema(description = "上次余额")
    private BigDecimal lastBalance;
    @Schema(description = "上次净值")
    private BigDecimal lastEquity;
    @Schema(description = "盈亏")
    private BigDecimal profit;
    @Schema(description = "已用保证金")
    private BigDecimal marginAmount;
    @Schema(description = "可用保证金")
    private BigDecimal freeMargin;
    @Schema(description = "保证金水平")
    private BigDecimal marginLevel;
    @Schema(description = "快照业务时间")
    private LocalDateTime snapshotTime;
}

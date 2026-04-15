package com.btg.commission.vo;

import com.btg.commission.entity.BtgMt5AccountSnapshot;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Redis 中 MT5 最新快照 JSON 载体（与 {@link Mt5SnapshotVO} 字段对齐，便于 Jackson 反序列化）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Mt5SnapshotCacheVO {

    private Long id;
    private Long userId;
    private String accountId;
    private String serverName;
    private BigDecimal balance;
    private BigDecimal equity;
    private BigDecimal lastBalance;
    private BigDecimal lastEquity;
    private BigDecimal profit;
    private BigDecimal marginAmount;
    private BigDecimal freeMargin;
    private BigDecimal marginLevel;
    private LocalDateTime snapshotTime;

    public static Mt5SnapshotCacheVO fromEntity(BtgMt5AccountSnapshot e) {
        if (e == null) {
            return null;
        }
        return Mt5SnapshotCacheVO.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .accountId(e.getAccountId())
                .serverName(e.getServerName())
                .balance(e.getBalance())
                .equity(e.getEquity())
                .lastBalance(e.getLastBalance())
                .lastEquity(e.getLastEquity())
                .profit(e.getProfit())
                .marginAmount(e.getMarginAmount())
                .freeMargin(e.getFreeMargin())
                .marginLevel(e.getMarginLevel())
                .snapshotTime(e.getSnapshotTime())
                .build();
    }

    public Mt5SnapshotVO toApiVo() {
        return Mt5SnapshotVO.builder()
                .id(id)
                .userId(userId)
                .accountId(accountId)
                .serverName(serverName)
                .balance(balance)
                .equity(equity)
                .lastBalance(lastBalance)
                .lastEquity(lastEquity)
                .profit(profit)
                .marginAmount(marginAmount)
                .freeMargin(freeMargin)
                .marginLevel(marginLevel)
                .snapshotTime(snapshotTime)
                .build();
    }
}

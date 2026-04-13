package com.btg.commission.dto.mt5;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Mt5SnapshotReportDTO {

    @NotBlank(message = "accountId 不能为空")
    @Size(max = 100)
    private String accountId;

    @NotBlank(message = "serverName 不能为空")
    @Size(max = 255)
    private String serverName;

    @NotNull(message = "lastBalance 不能为空")
    private BigDecimal lastBalance;

    @NotNull(message = "balance 不能为空")
    private BigDecimal balance;

    @NotNull(message = "lastEquity 不能为空")
    private BigDecimal lastEquity;

    @NotNull(message = "equity 不能为空")
    private BigDecimal equity;

    private BigDecimal profit;
    private BigDecimal marginAmount;
    private BigDecimal freeMargin;
    private BigDecimal marginLevel;

    /** 可选；不传则使用服务端接收时间。支持 {@code yyyy-MM-dd HH:mm:ss} 与 ISO（含 {@code T}）。 */
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime snapshotTime;
}

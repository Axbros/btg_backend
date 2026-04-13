package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "当前用户可归仓的补仓单（审核通过或部分归还且剩余应还大于 0）")
public class RepayableReplenishmentVO {

    private Long id;

    @Schema(description = "补仓单号 apply_no")
    private String applyNo;

    private BigDecimal approvedAmount;
    private BigDecimal repaidAmount;
    private BigDecimal pendingRepayAmount;
    private BigDecimal remainingAmount;

    @Schema(description = "补仓终审时间")
    private LocalDateTime auditTime;

    private String transferScreenshotUrl;
    private String transferRemark;
}

package com.btg.commission.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ReplenishmentApplyVO {

    private Long id;
    private String applyNo;
    private Long userId;
    private BigDecimal principalAmount;
    private BigDecimal balanceAmount;
    private BigDecimal replenishAmount;
    private String balanceScreenshotUrl;
    /** 资方补仓转账凭证 */
    private String transferScreenshotUrl;
    /** 资方补仓转账备注 */
    private String transferRemark;
    private Integer status;
    private BigDecimal approvedAmount;
    private BigDecimal repaidAmount;
    private BigDecimal pendingRepayAmount;
    private BigDecimal remainingAmount;
    private LocalDateTime submitTime;
    private LocalDateTime auditTime;
    private Long auditBy;
    private String auditRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

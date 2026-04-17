package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "申请人昵称（来自 btg_user）")
    private String nickname;

    @Schema(description = "申请人手机号（来自 btg_user）")
    private String mobile;
    private BigDecimal principalAmount;
    private BigDecimal balanceAmount;
    private BigDecimal replenishAmount;
    private String balanceScreenshotUrl;
    /** 资方补仓转账凭证 */
    private String transferScreenshotUrl;
    /** 资方补仓转账备注 */
    private String transferRemark;
    private Integer status;

    @Schema(description = "申请人资料：券商/交易所名称（如币安）")
    private String walletName;

    @Schema(description = "申请人资料：钱包地址")
    private String walletAddress;

    @Schema(description = "资方受理时间；null 表示尚未受理")
    private LocalDateTime acceptedAt;

    @Schema(description = "资方受理人 user id")
    private Long acceptedBy;

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

    private Long currentHandlerUserId;
    private Long assignedCapitalUserId;
    @Schema(description = "资方执行人昵称，无则 null")
    private String assignedCapitalNickname;
    private Long assignedBy;
    private LocalDateTime assignedTime;
    private String assignRemark;
    private LocalDateTime capitalSubmitTime;
    private String capitalSubmitRemark;
    private String capitalReceiverUid;
    private Integer arrivalConfirmStatus;
    private LocalDateTime arrivalConfirmTime;
    private Long arrivalConfirmBy;
    private String arrivalConfirmRemark;
}

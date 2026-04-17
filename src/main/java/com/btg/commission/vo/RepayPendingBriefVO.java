package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "归仓列表项；含关联补仓单与资方执行方摘要")
public class RepayPendingBriefVO {

    private Long id;

    @Schema(description = "归仓单号 repay_no")
    private String repayNo;

    @Schema(description = "状态码：1 待资方审核；2 审核通过；3 终局拒绝；4 退回申请人")
    private Integer status;

    @Schema(description = "关联补仓申请 id")
    private Long replenishApplyId;

    @Schema(description = "关联补仓单号 apply_no")
    private String replenishApplyNo;

    @Schema(description = "关联补仓单：核准金额")
    private BigDecimal replenishApprovedAmount;
    @Schema(description = "关联补仓单：已归还")
    private BigDecimal replenishRepaidAmount;
    @Schema(description = "关联补仓单：待审核归仓占用")
    private BigDecimal replenishPendingRepayAmount;
    @Schema(description = "关联补仓单：剩余应还")
    private BigDecimal replenishRemainingAmount;

    @Schema(description = "关联补仓核准金额（与 replenishApprovedAmount 同义）")
    private BigDecimal approvedAmount;
    @Schema(description = "关联补仓已归还")
    private BigDecimal repaidAmount;
    @Schema(description = "关联补仓待审核归仓占用")
    private BigDecimal pendingRepayAmount;
    @Schema(description = "关联补仓剩余应还")
    private BigDecimal remainingAmount;

    @Schema(description = "补仓执行方 / 归仓审核人")
    private Long capitalUserId;
    @Schema(description = "补仓执行方昵称")
    private String capitalUserName;
    @Schema(description = "补仓执行方收款 UID 快照")
    private String capitalReceiverUid;

    private Long currentHandlerUserId;
    @Schema(description = "当前处理人昵称")
    private String currentHandlerUserName;

    @Schema(description = "提交次数")
    private Integer submitVersion;

    @Schema(description = "最近一次拒绝原因")
    private String lastRejectReason;
}

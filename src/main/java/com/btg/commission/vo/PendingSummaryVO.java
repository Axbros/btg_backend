package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录用户待办数量汇总（不含金额）")
public class PendingSummaryVO {

    @Schema(description = "是否存在任一待办，等价于 totalPendingCount > 0")
    private Boolean hasPending;

    @Schema(description = "待审核的下级结算单数量（收款人为本人、status=待上级审核）")
    private Integer pendingSettlementReviewCount;

    @Schema(description = "待审核的下级利润上报数量（直属上级为本人、status=待直属上级审核）")
    private Integer pendingProfitReportReviewCount;

    @Schema(description = "待支付给上级的结算单数量（本人为付款人、status=待提交凭证或待上级审核；与结算 mine-payables 列表口径一致）")
    private Integer pendingSettlementPayableCount;

    @Schema(description = "待审核补仓申请数量；仅根用户/资方统计，否则为 0")
    private Integer pendingReplenishmentReviewCount;

    @Schema(description = "待审核归仓申请数量；仅根用户/资方统计，否则为 0")
    private Integer pendingReplenishmentRepayReviewCount;

    @Schema(description = "被退回待修改的利润上报数量（本人为申报人）")
    private Integer returnedProfitReportCount;

    @Schema(description = "被退回待修改的补仓申请数量（本人为申请人）")
    private Integer returnedReplenishmentApplyCount;

    @Schema(description = "被退回待修改的归仓申请数量（本人为申请人）")
    private Integer returnedReplenishmentRepayCount;

    @Schema(description = "上述八项之和（含待支付给上级与退回待改）")
    private Integer totalPendingCount;
}

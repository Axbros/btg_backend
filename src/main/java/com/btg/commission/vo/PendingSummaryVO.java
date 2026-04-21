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

    @Schema(description = "待支付给上级的结算单数量（本人为付款人、待提交凭证或待上级审核；待办统计口径，与 GET …/settlements/mine-payables 无参全量列表不同）")
    private Integer pendingSettlementPayableCount;

    @Schema(description = "待审核补仓申请数量；仅根用户/资方统计，否则为 0")
    private Integer pendingReplenishmentReviewCount;

    @Schema(description = "待系统管理员资格审核的新成员数量（user_profile.qualification_status=待审）；仅根用户统计，否则为 0")
    private Integer pendingQualificationReviewCount;

    @Schema(description = "待审核归仓申请数量（本人为补仓执行方且归仓状态待资方审核）")
    private Integer pendingReplenishmentRepayReviewCount;

    @Schema(description = "待确认补仓到账数量（本人为补仓申请人，状态为待申请人确认到账）")
    private Integer pendingReplenishmentApplicantConfirmCount;

    @Schema(description = "被退回待修改的利润上报数量（本人为当前待处理人：链上拒单为被拒结算付款人，直属拒单为申报人；兼容 current_handler 为空时回落申报人）")
    private Integer returnedProfitReportCount;

    @Schema(description = "被退回待修改的补仓申请数量（本人为申请人）")
    private Integer returnedReplenishmentApplyCount;

    @Schema(description = "被退回待修改的归仓申请数量（本人为申请人）")
    private Integer returnedReplenishmentRepayCount;

    @Schema(description = "上述各项之和（含待审结算、待支付给上级、待确认补仓到账、根用户待审新成员资格与退回待改）")
    private Integer totalPendingCount;
}

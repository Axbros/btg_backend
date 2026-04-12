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

    @Schema(description = "待审核补仓申请数量；仅根用户/资方统计，否则为 0")
    private Integer pendingReplenishmentReviewCount;

    @Schema(description = "待审核归仓申请数量；仅根用户/资方统计，否则为 0")
    private Integer pendingReplenishmentRepayReviewCount;

    @Schema(description = "上述四项之和")
    private Integer totalPendingCount;
}

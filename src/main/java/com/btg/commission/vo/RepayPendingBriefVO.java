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
@Schema(description = "归仓列表项（本人/资方待审）；含关联补仓单摘要")
public class RepayPendingBriefVO {

    private Long id;

    @Schema(description = "归仓单号 repay_no")
    private String repayNo;

    @Schema(description = "状态码：1 待审核；2 审核通过；3 审核拒绝")
    private Integer status;

    @Schema(description = "关联补仓申请 id")
    private Long replenishApplyId;

    @Schema(description = "关联补仓单号 apply_no")
    private String replenishApplyNo;

    private BigDecimal replenishApprovedAmount;
    private BigDecimal replenishRepaidAmount;
    private BigDecimal replenishPendingRepayAmount;
    private BigDecimal replenishRemainingAmount;
}

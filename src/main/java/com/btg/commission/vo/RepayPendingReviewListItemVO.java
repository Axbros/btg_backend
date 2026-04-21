package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
@Schema(description = "待资方审核归仓列表项（精简）")
public class RepayPendingReviewListItemVO {

    Long id;

    String repayNo;

    @Schema(description = "关联补仓单 pending_repay_amount（待审核归仓占用）")
    BigDecimal pendingRepayAmount;
}

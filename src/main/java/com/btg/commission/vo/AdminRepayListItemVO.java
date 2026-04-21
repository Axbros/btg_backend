package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class AdminRepayListItemVO {

    Long id;

    String repayNo;

    @Schema(description = "归仓状态：1 待资方审核；2 通过；3 拒绝；4 退回申请人")
    Integer status;

    @Schema(description = "关联补仓单 pending_repay_amount（待审核归仓占用）")
    BigDecimal replenishPendingRepayAmount;

    @Schema(description = "归仓申请人：昵称优先，否则手机号")
    String applicantNickname;
}

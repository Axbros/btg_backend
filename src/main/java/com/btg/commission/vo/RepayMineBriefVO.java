package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class RepayMineBriefVO {

    Long id;

    String repayNo;

    @Schema(description = "归仓状态码：1 待资方审核；2 审核通过；3 终局拒绝；4 退回申请人")
    Integer status;

    @Schema(description = "本笔归仓申请金额 repay_amount")
    BigDecimal repayAmount;
}

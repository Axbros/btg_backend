package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AccountSummaryVo {

    @Schema(description = "累积盈利：下级审核通过累加 net_amount（盈利×比例）；上级审核通过累加 commission_amount（盈利×(1−比例)）")
    private BigDecimal totalProfitAmount;

    @Schema(description = "下级：审核通过累加「分出」commission_amount（盈利×(1−比例)）")
    private BigDecimal totalCommissionOutAmount;

    @Schema(description = "上级：审核通过「收到佣金」= commission_amount 之和（盈利×(1−比例)）")
    private BigDecimal totalCommissionInAmount;

    @Schema(description = "下级：待审核分出佣金，各待审申报 commission_amount（盈利×(1−比例)）；上级为 0")
    private BigDecimal pendingCommissionOutAmount;

    @Schema(description = "下级：待审核应收（自留分成），各待审申报 net_amount（盈利×比例）；上级：待审核应收（收下级的），各待审 commission_amount（盈利×(1−比例)）")
    private BigDecimal pendingCommissionInAmount;
}

package com.btg.commission.vo;

import com.btg.commission.enums.CommissionRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "我的佣金 — 列表项（精简）；详情见 GET /api/commissions/mine/{id}")
public class CommissionMineListItemVo {

    @Schema(description = "佣金流水 ID，用于拉取详情")
    private Long id;

    @Schema(description = "关联收益申报单号")
    private String profitRecordNo;

    @Schema(description = "申报盈利金额")
    private BigDecimal profitAmount;

    @Schema(description = "佣金金额")
    private BigDecimal commissionAmount;

    @Schema(description = "分佣比例快照")
    private BigDecimal commissionRate;

    @Schema(description = "状态")
    private CommissionRecordStatus status;
}

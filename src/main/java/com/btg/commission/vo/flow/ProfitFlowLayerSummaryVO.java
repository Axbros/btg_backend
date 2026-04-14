package com.btg.commission.vo.flow;

import com.btg.commission.enums.ProfitFlowLayerState;
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
@Schema(description = "利润链单层摘要：当前层待审/通过/拒绝等，不含结算单审核流水")
public class ProfitFlowLayerSummaryVO {

    @Schema(description = "DIRECT_PROFIT_REVIEW=直属审利润单；SETTLEMENT=逐级打款")
    private String layerType;
    /** 结算层级，与 btg_settlement_order.level_no 一致；直属审利润为 null */
    private Integer settlementLevelNo;
    private Long fromUserId;
    private Long toUserId;
    private String fromDisplayName;
    private String toDisplayName;
    private BigDecimal payAmount;
    private ProfitFlowLayerState state;
}

package com.btg.commission.vo.flow;

import com.btg.commission.enums.ReplenishmentStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplenishmentApplyFlowSummaryVO {

    private Long id;
    private String applyNo;
    private ReplenishmentStatusEnum status;
    private BigDecimal replenishAmount;
    private BigDecimal remainingAmount;
}

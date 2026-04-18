package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "补仓申请列表摘要（id、单号、状态、补仓金额、提交时间）")
public class ReplenishmentApplyBriefVO {

    private Long id;

    @Schema(description = "补仓申请单号 apply_no")
    private String applyNo;

    @Schema(description = "状态码，与 ReplenishmentStatusEnum 一致")
    private Integer status;

    @Schema(description = "申请补仓金额 replenish_amount")
    private BigDecimal replenishAmount;

    @Schema(description = "提交时间 submit_time")
    private LocalDateTime submitTime;
}

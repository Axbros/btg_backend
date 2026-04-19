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
@Schema(description = "补仓申请列表摘要（含后台 status 与申请人简化状态）")
public class ReplenishmentApplyBriefVO {

    private Long id;

    @Schema(description = "补仓申请单号 apply_no")
    private String applyNo;

    @Schema(description = "后台状态码 ReplenishmentStatusEnum：1～8，与库表一致")
    private Integer status;

    @Schema(description = "申请人简化状态，仅 GET …/replenishments/mine 返回：1 待审核；2 待确认到账；3 已成功；4 已拒绝；5 已关闭。后台 1/2/3/5 归入 1")
    private Integer userVisibleStatus;

    @Schema(description = "申请补仓金额 replenish_amount")
    private BigDecimal replenishAmount;

    @Schema(description = "提交时间 submit_time")
    private LocalDateTime submitTime;
}

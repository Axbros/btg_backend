package com.btg.commission.vo;

import com.btg.commission.enums.ProfitRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "直属上级 — 收益申报列表项（精简字段）")
public class ReferrerProfitListItemVo {

    @Schema(description = "申报单 ID，用于 GET /api/profits/referrer/records/{id}")
    private Long id;

    @Schema(description = "申报单号")
    private String recordNo;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "申报人手机号（库中无或查不到用户时为 null）")
    private String userMobile;

    @Schema(description = "申报盈利金额")
    private BigDecimal profitAmount;

    @Schema(description = "分佣比例快照")
    private BigDecimal commissionRate;

    @Schema(description = "分给上级：盈利 × (1 − 分佣比例)（commission_amount）")
    private BigDecimal dueShareAmount;

    @Schema(description = "申报人自留：盈利 × 分佣比例（net_amount）")
    private BigDecimal netAmount;

    @Schema(description = "状态：1待审 2通过 3拒绝")
    private ProfitRecordStatus status;
}

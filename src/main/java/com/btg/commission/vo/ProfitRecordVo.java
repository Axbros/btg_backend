package com.btg.commission.vo;

import com.btg.commission.enums.ProfitRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "收益申报单")
public class ProfitRecordVo {

    @Schema(description = "申报单 ID")
    private Long id;

    @Schema(description = "申报单号")
    private String recordNo;

    @Schema(description = "申报人用户 ID")
    private Long userId;

    @Schema(description = "直属推荐人用户 ID（应收佣金方）")
    private Long referrerUserId;

    @Schema(description = "策略 ID")
    private Long strategyId;

    @Schema(description = "申报盈利金额")
    private BigDecimal profitAmount;

    @Schema(description = "分佣比例快照")
    private BigDecimal commissionRate;

    @Schema(description = "分给上级部分：盈利 × (1 − 分佣比例)；对应申报人待审核分出、上级待审核应收")
    private BigDecimal commissionAmount;

    @Schema(description = "申报人自留分成：盈利 × 分佣比例；对应申报人待审核应收")
    private BigDecimal netAmount;

    @Schema(description = "收益截图 URL")
    private String profitScreenshotUrl;

    @Schema(description = "转账截图 URL")
    private String transferScreenshotUrl;

    @Schema(description = "状态：1待审 2通过 3拒绝")
    private ProfitRecordStatus status;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "审核人用户 ID")
    private Long auditBy;

    @Schema(description = "审核备注")
    private String auditRemark;
}

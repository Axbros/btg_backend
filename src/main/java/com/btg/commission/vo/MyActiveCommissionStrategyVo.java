package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "当前登录用户适用的分佣策略（直属上级已绑定的有效策略快照）")
public class MyActiveCommissionStrategyVo {

    @Schema(description = "策略 ID")
    private Long strategyId;

    @Schema(description = "策略名称（策略已删时可能为 null）")
    private String strategyName;

    @Schema(description = "策略编码（策略已删时可能为 null）")
    private String strategyCode;

    @Schema(description = "分佣比例快照，如 0.4000 表示申报人自留 40%（net 口径）")
    private BigDecimal commissionRate;

    @Schema(description = "分给上级的比例（1 − 分佣比例），转账截图基数同此侧金额占比")
    private BigDecimal transferRatio;

    @Schema(description = "仅当请求传入 profitAmount 时返回：传入的申报盈利预览值")
    private BigDecimal previewProfitAmount;

    @Schema(description = "预览：分给上级（盈利×(1−比例)，与 commission_amount、待审核分出/上级待审应收 同口径）")
    private BigDecimal previewCommissionAmount;

    @Schema(description = "预览：同 previewCommissionAmount（转账截图参考）")
    private BigDecimal previewTransferAmount;

    @Schema(description = "预览：申报人自留（盈利×比例，与 net_amount、申报人待审核应收 同口径）")
    private BigDecimal previewNetAmount;
}

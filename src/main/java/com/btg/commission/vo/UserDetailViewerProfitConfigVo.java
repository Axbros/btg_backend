package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * {@code GET /user/{id}} 中与当前登录用户相关的分润比例：兜底 / 不兜底两档及当前模式说明。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "当前用户在详情页上下文下的分润比例配置（ACTIVE）")
public class UserDetailViewerProfitConfigVo {

    @Schema(description = "兜底模式：子级可分总利润比例（0～1）")
    private BigDecimal guaranteeRatio;

    @Schema(description = "不兜底模式：子级可分总利润比例（0～1）")
    private BigDecimal nonGuaranteeRatio;

    @Schema(description = "上级为该边配置的当前模式：GUARANTEE / NON_GUARANTEE")
    private String commissionMode;

    @Schema(description = "模式中文：兜底 / 不兜底")
    private String commissionModeDesc;

    @Schema(description = "当前登录用户为下级配置「兜底」子级可分比例时允许的最大值（0～1）；与分润配置接口校验上限一致")
    private BigDecimal maxAssignableChildGuaranteeRatio;

    @Schema(description = "当前登录用户为下级配置「不兜底」子级可分比例时允许的最大值（0～1）")
    private BigDecimal maxAssignableChildNonGuaranteeRatio;
}

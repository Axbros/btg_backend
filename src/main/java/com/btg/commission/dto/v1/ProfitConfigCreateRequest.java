package com.btg.commission.dto.v1;

import com.btg.commission.enums.CommissionModeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "直属上级为直属下级创建分润配置（含兜底/不兜底双比例与当前生效模式）")
public class ProfitConfigCreateRequest {

    @NotNull
    @Schema(description = "直属下级用户 id")
    private Long childUserId;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("1")
    private BigDecimal guaranteeRatio;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("1")
    private BigDecimal nonGuaranteeRatio;

    @NotNull
    @Schema(description = "当前生效分润模式（由上级设置，非下级在上报时选择）")
    private CommissionModeEnum commissionMode;
}

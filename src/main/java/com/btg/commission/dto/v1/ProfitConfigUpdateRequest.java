package com.btg.commission.dto.v1;

import com.btg.commission.enums.CommissionModeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "直属上级更新直属下级分润配置")
public class ProfitConfigUpdateRequest {

    @NotNull
    @DecimalMin("0")
    @DecimalMax("1")
    private BigDecimal guaranteeRatio;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("1")
    private BigDecimal nonGuaranteeRatio;

    @NotNull
    @Schema(description = "当前生效分润模式")
    private CommissionModeEnum commissionMode;
}

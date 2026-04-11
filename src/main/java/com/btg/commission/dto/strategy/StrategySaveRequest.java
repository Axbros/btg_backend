package com.btg.commission.dto.strategy;

import com.btg.commission.enums.StrategyStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StrategySaveRequest {

    @NotBlank
    private String strategyCode;

    @NotBlank
    private String strategyName;

    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal commissionRate;

    private String description;

    @NotNull
    private StrategyStatus status;

    @NotNull
    private Integer sortNo;
}

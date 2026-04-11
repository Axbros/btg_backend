package com.btg.commission.dto.v1;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProfitConfigCreateRequest {

    @NotNull
    private Long childUserId;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("1")
    private BigDecimal childProfitRatio;
}

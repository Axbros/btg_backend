package com.btg.commission.dto.profit;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProfitSubmitRequest {

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal profitAmount;

    @NotBlank
    private String profitScreenshotUrl;

    @NotBlank
    private String transferScreenshotUrl;
}

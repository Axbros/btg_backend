package com.btg.commission.dto.v1;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProfitReportSubmitRequest {

    @NotNull
    @DecimalMin(value = "0.01", message = "利润金额须大于 0")
    private BigDecimal profitAmount;

    @NotBlank
    private String profitScreenshotUrl;

    @NotBlank
    private String transferToParentScreenshotUrl;
}

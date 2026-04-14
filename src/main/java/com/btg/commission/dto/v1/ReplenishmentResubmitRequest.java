package com.btg.commission.dto.v1;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReplenishmentResubmitRequest {

    @NotNull(message = "当前余额不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "当前余额不能为负")
    private BigDecimal balanceAmount;

    @NotBlank(message = "余额截图不能为空")
    @Size(max = 500)
    private String balanceScreenshotUrl;
}

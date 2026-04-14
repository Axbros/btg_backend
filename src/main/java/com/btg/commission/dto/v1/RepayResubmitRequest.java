package com.btg.commission.dto.v1;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RepayResubmitRequest {

    @NotNull(message = "归仓金额不能为空")
    @DecimalMin(value = "0.00", inclusive = false, message = "归仓金额须大于 0")
    private BigDecimal repayAmount;

    @NotBlank(message = "归仓截图不能为空")
    @Size(max = 500)
    private String repayScreenshotUrl;
}

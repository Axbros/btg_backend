package com.btg.commission.dto.v1;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProfitReportResubmitRequest {

    @NotNull(message = "利润金额不能为空")
    @DecimalMin(value = "0.00", inclusive = false, message = "利润金额须大于 0")
    private BigDecimal profitAmount;

    @NotBlank(message = "收益截图不能为空")
    @Size(max = 500)
    private String profitScreenshotUrl;

    @NotBlank(message = "给直属上级的转账截图不能为空")
    @Size(max = 500)
    private String transferScreenshotUrl;
}

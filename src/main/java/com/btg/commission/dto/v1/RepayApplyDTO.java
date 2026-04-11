package com.btg.commission.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RepayApplyDTO {

    @NotNull(message = "归还金额不能为空")
    private BigDecimal repayAmount;

    @NotBlank(message = "归仓转账截图不能为空")
    private String repayScreenshotUrl;
}

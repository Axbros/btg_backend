package com.btg.commission.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReplenishmentApplyDTO {

    @NotNull(message = "当前余额不能为空")
    private BigDecimal balanceAmount;

    /** 余额截图 URL */
    @NotBlank(message = "余额截图不能为空")
    private String balanceScreenshotUrl;
}

package com.btg.commission.dto.v1;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SettlementSubmitRequest {

    @NotBlank
    private String transferScreenshotUrl;
}

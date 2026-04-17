package com.btg.commission.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReplenishmentCapitalSubmitRequest {

    @NotBlank(message = "转账凭证不能为空")
    private String transferScreenshotUrl;

    @Size(max = 500)
    private String transferRemark;

    @NotBlank(message = "资方收款 UID 不能为空")
    @Size(max = 100)
    private String capitalReceiverUid;
}

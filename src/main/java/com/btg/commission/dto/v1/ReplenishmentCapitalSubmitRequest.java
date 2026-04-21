package com.btg.commission.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "资方提交补仓凭证；收款 UID 取资方执行人 btg_user_profile.exchange_uid，无需传参")
public class ReplenishmentCapitalSubmitRequest {

    @NotBlank(message = "转账凭证不能为空")
    private String transferScreenshotUrl;

    @Size(max = 500)
    private String transferRemark;
}

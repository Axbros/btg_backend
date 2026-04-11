package com.btg.commission.dto.v1;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReplenishmentApproveDTO {

    /** 资方给申请人的补仓转账凭证 URL */
    @NotBlank(message = "请上传或填写资方转账凭证地址")
    private String transferScreenshotUrl;

    /** 资方补仓转账备注（选填） */
    private String transferRemark;
}

package com.btg.commission.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 根用户/管理员同意补仓：须上传转账凭证（与资方提交口径一致）；审核备注可选。
 */
@Data
public class AdminReplenishmentApproveRequest {

    /** 审核备注（可选），写入 audit_remark */
    @Size(max = 500)
    private String remark;

    @NotBlank(message = "转账凭证不能为空")
    private String transferScreenshotUrl;

    /** 转账备注（可选） */
    @Size(max = 500)
    private String transferRemark;

}

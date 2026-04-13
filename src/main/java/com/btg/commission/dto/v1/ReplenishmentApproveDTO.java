package com.btg.commission.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ReplenishmentApproveDTO {

    /**
     * 资方给申请人的补仓转账凭证 URL。
     * 状态 7 首次上传时必填；状态 8 待终审前再次编辑时选填（不传则保留原凭证）。
     */
    @Schema(description = "资方转账凭证 URL；待上传凭证(7)时必填，待终审(8)修改时可省略以仅改备注")
    private String transferScreenshotUrl;

    /** 资方补仓转账备注（选填） */
    private String transferRemark;
}

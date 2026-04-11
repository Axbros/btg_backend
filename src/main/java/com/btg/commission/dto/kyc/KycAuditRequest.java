package com.btg.commission.dto.kyc;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycAuditRequest {

    /** 被审核用户（下级）的用户 ID */
    @NotNull
    private Long targetUserId;

    private String remark;
}

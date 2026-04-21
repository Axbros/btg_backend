package com.btg.commission.vo;

import com.btg.commission.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
@Schema(description = "待系统管理员资格审核的用户行（列表精简字段）")
public class PendingQualificationUserVO {

    @Schema(description = "用户 id（btg_user.id），用于 POST …/approve-qualification、reject-qualification")
    Long id;

    String nickname;

    String mobile;

    @Schema(description = "用户账号状态：-1 资料未完善；0 待上级审核；1 正常（与 btg_user.status 一致）")
    UserStatus status;

    BigDecimal principalAmount;
}

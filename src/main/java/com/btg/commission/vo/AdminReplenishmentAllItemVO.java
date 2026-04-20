package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class AdminReplenishmentAllItemVO {

    /** 跳转 GET …/admin/replenishments/{id}；与 applyNo 一一对应 */
    Long id;

    String applyNo;
    BigDecimal replenishAmount;

    @Schema(description = "后台状态码 1～8，与 ReplenishmentStatusEnum 一致")
    Integer status;

    @Schema(description = "申请人昵称")
    String nickname;
}

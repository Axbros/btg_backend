package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "下级补仓/归仓列表统一行结构。补仓：id=补仓申请、status=补仓状态、replenishAmount=replenish_amount。归仓：id=归仓申请、status=1待审2通过3拒绝、replenishAmount=repay_amount（归还申报金额）。")
public class ReplenishmentTeamItemVO {

    @Schema(description = "补仓申请 id 或 归仓申请 id（视接口而定）")
    private Long id;

    @Schema(description = "补仓状态或归仓状态码（视接口而定）")
    private Integer status;

    private String nickname;
    private String mobile;

    @Schema(description = "补仓接口为 replenish_amount；归仓 repays/team 接口为 repay_amount")
    private BigDecimal replenishAmount;
}

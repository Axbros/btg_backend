package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "资方待处理补仓列表项（仅摘要；完整信息见 GET …/admin/replenishments/{id}）")
public class ReplenishmentPendingBriefVO {

    private Long id;
    private String nickname;
    private String mobile;
    private BigDecimal replenishAmount;
}

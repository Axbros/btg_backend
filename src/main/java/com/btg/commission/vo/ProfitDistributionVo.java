package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 分润明细行（按上报单查询）；收益人展示名仅根用户可见。 */
@Data
@Builder
public class ProfitDistributionVo {

    private Long id;
    private Long reportId;
    private Long beneficiaryUserId;
    private Integer levelNo;
    private BigDecimal upperRatio;
    private BigDecimal lowerRatio;
    private BigDecimal incomeAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Schema(description = "收益人昵称；昵称为空时为手机号。仅根用户有值，其余为 null")
    private String beneficiaryDisplayName;
}

package com.btg.commission.vo;

import com.btg.commission.enums.CommissionRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "佣金流水详情（GET /api/commissions/mine/{id}）")
public class CommissionRecordVo {

    private Long id;
    private Long profitRecordId;
    private Long fromUserId;
    private Long toUserId;
    private Long strategyId;
    private BigDecimal commissionRate;
    private BigDecimal profitAmount;
    private BigDecimal commissionAmount;
    private CommissionRecordStatus status;
    private LocalDateTime confirmedTime;

    @Schema(description = "关联收益申报单号")
    private String profitRecordNo;

    @Schema(description = "来源用户（申报人）昵称")
    private String fromNickname;

    @Schema(description = "来源用户（申报人）手机号")
    private String fromMobile;

    @Schema(description = "分佣策略名称")
    private String strategyName;
}

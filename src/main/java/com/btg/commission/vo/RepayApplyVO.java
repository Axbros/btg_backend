package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RepayApplyVO {

    private Long id;
    private String repayNo;
    private Long replenishApplyId;
    private Long userId;

    @Schema(description = "申请人 btg_user.nickname")
    private String nickname;

    @Schema(description = "申请人 btg_user.mobile")
    private String mobile;

    @Schema(description = "关联补仓单 replenishApplyId 对应完整信息（与补仓模块 VO 一致）")
    private ReplenishmentApplyVO replenishmentApply;

    private BigDecimal repayAmount;
    private String repayScreenshotUrl;
    private Integer status;
    private LocalDateTime submitTime;
    private LocalDateTime auditTime;
    private Long auditBy;
    private String auditRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

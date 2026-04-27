package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 直属上级「待审核利润单」列表行 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "待直属上级审核的利润上报摘要")
public class ProfitReportPendingReviewItemVO {

    private Long id;
    private String reportNo;
    private Long reportUserId;
    private BigDecimal profitAmount;
    /** 与 {@link com.btg.commission.enums.ProfitReportStatus#getValue()} 一致 */
    private Integer status;
    private LocalDateTime submitTime;

    @Schema(description = "利润单快照：GUARANTEE / NON_GUARANTEE")
    private String commissionMode;

    @Schema(description = "分润模式中文")
    private String commissionModeDesc;
}

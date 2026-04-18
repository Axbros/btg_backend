package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "本人利润上报列表项（分页摘要）")
public class ProfitReportMineBriefVO {

    private Long id;

    @Schema(description = "申报单号 report_no")
    private String reportNo;

    @Schema(description = "状态码，与 ProfitReportStatus 一致")
    private Integer status;

    private LocalDateTime submitTime;

    private BigDecimal profitAmount;
}

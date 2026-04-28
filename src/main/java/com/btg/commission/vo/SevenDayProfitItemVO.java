package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 近 7 日利润图表：与 {@link com.btg.commission.service.ProfitReportService#listMineSevenDayProfit} 对齐，按上海自然日汇总。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "单日利润（图表）")
public class SevenDayProfitItemVO {

    @Schema(description = "周几简称，如 周一、周二", example = "周一")
    private String date;

    @Schema(description = "自然日 yyyy-MM-dd（上海时区）", example = "2026-04-14")
    private String dateKey;

    @Schema(description = "该日已上报利润合计，无单则为 0")
    private BigDecimal profit;
}

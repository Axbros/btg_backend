package com.btg.commission.vo;

import com.btg.commission.entity.ProfitReport;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 利润单详情：在 {@link ProfitReport} 基础上补充模式中文说明（实体字段已含 {@code commissionMode}）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "利润上报详情")
public class ProfitReportDetailVO {

    @JsonUnwrapped
    private ProfitReport report;

    @Schema(description = "分润模式中文：兜底 / 不兜底；历史单无快照时可能为 null")
    private String commissionModeDesc;
}

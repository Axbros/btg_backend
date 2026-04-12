package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "归仓列表项（id、单号、状态；用于资方待审列表与本人归仓列表）")
public class RepayPendingBriefVO {

    private Long id;

    @Schema(description = "归仓单号 repay_no")
    private String repayNo;

    @Schema(description = "状态码：1 待审核；2 审核通过；3 审核拒绝")
    private Integer status;
}

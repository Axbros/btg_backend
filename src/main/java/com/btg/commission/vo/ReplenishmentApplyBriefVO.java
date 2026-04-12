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
@Schema(description = "我的补仓申请列表项（id、单号、状态）")
public class ReplenishmentApplyBriefVO {

    private Long id;

    @Schema(description = "补仓申请单号 apply_no")
    private String applyNo;

    @Schema(description = "状态码，与补仓模块一致：1 待审核；2 审核通过；3 审核拒绝；4 部分归还；5 已结清；6 已关闭")
    private Integer status;
}

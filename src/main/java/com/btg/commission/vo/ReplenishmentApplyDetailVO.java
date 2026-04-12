package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "补仓申请详情（本人）：完整补仓信息 + 已成功归仓记录")
public class ReplenishmentApplyDetailVO {

    @Schema(description = "补仓申请完整信息")
    private ReplenishmentApplyVO replenishment;

    @Builder.Default
    @Schema(description = "关联本补仓单且审核通过（归仓成功）的归仓申请列表；无则为空数组")
    private List<RepayApplyVO> approvedRepays = new ArrayList<>();
}

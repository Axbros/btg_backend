package com.btg.commission.vo.flow;

import com.btg.commission.enums.ProfitReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "按当前用户可见范围裁剪的利润链层级摘要（仅到哪一层、该层状态；不含结算单审核流水）")
public class SettlementScopedProfitFlowVO {

    private Long rootReportId;
    private Long reportUserId;
    private String reportNo;
    private ProfitReportStatus reportStatus;
    private Long currentHandlerUserId;
    private Boolean returnedToApplicant;
    /**
     * 自下而上（申报人→…）可见链路上的用户 id，便于前端对齐层级顺序。
     */
    private List<Long> visibleUserIdsInOrder;
    /**
     * 当前用户可见范围内的层级状态：先直属审利润（若可见），再逐级结算（仅两端均在可见集内的层）。
     */
    private List<ProfitFlowLayerSummaryVO> layers;
    /** FULL=申报人或根；PATH=邀请链上级；SUBTREE=子树与链交集 */
    private String scopeType;
}

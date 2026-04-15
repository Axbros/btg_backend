package com.btg.commission.vo.flow;

import com.btg.commission.enums.ProfitReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 利润单分润链路详情：总利润切片 + 逐级结算状态 + 当前处理人（按访问者裁剪敏感金额）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "利润分润链路详情")
public class ProfitFlowDetailVO {

    private Long reportId;
    private String reportNo;
    private Long reportUserId;
    private String reportUserName;
    private BigDecimal profitAmount;

    private Long currentHandlerUserId;
    private String currentHandlerUserName;
    private ProfitReportStatus status;
    private String flowStatus;
    private Integer submitVersion;
    private String lastRejectReason;

    /**
     * FULL_FINANCIAL=根用户，返回全链路与全量金额；
     * ANCESTOR_SUBCHAIN_FINANCIAL=申报人邀请链上的非根上级，仅返回「本层及以下」切片与金额；
     * REPORTER_SUBCHAIN=申报人，仅本人切片层 + {@code directParent*} 直属上级处理摘要；
     * CHAIN_PARTICIPANT_SUBCHAIN=链上其他参与方，自链上位置向下切片，之上不返回。
     */
    private String dataScope;

    /** 与利润单或当前卡住的结算步骤一致的状态码（多为枚举名或业务 flow 字段） */
    private String currentFlowStatus;
    private String currentFlowStatusDesc;
    private String pendingAction;
    private String pendingActorDisplayName;

    /**
     * 仅申报人视角：直属上级（邀请链）对本人利润/首笔上缴的处理摘要，便于了解待审/通过/拒绝/退回等。
     */
    @Schema(description = "申报人可见：直属上级处理状态码")
    private String directParentStatus;
    @Schema(description = "申报人可见：直属上级处理状态说明")
    private String directParentStatusDesc;
    @Schema(description = "申报人可见：与直属上级相关的待办动作码")
    private String directParentAction;
    @Schema(description = "申报人可见：直属上级展示名（审核人角色时）")
    private String directParentReviewerName;
    @Schema(description = "申报人可见：直属上级拒绝/退回等备注")
    private String directParentRemark;
    @Schema(description = "申报人可见：直属上级最近操作时间")
    private LocalDateTime directParentOperateTime;

    private List<ProfitFlowLayerVO> layers;
}

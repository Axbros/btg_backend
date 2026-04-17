package com.btg.commission.vo.flow;

import com.btg.commission.enums.RepayStatusEnum;
import com.btg.commission.vo.RepayApplyVO;
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
@Schema(description = "归仓申请状态流详情")
public class RepayApplyFlowDetailVO {

    private RepayApplyVO repay;
    private ReplenishmentApplyFlowSummaryVO linkedReplenishment;
    private Long applicantUserId;
    private String applicantNickname;
    private Long currentHandlerUserId;
    private String currentHandlerUserName;
    private RepayStatusEnum currentStatus;
    private Boolean returnedToApplicant;
    private Boolean everRejected;
    private Integer submitVersion;
    private String lastRejectReason;

    @Schema(description = "补仓执行方 / 归仓审核人")
    private Long capitalUserId;
    @Schema(description = "补仓执行方昵称")
    private String capitalUserName;
    @Schema(description = "补仓执行方收款 UID 快照")
    private String capitalReceiverUid;

    private List<BusinessFlowNodeVO> nodes;
}

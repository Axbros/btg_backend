package com.btg.commission.vo.flow;

import com.btg.commission.enums.ReplenishmentStatusEnum;
import com.btg.commission.vo.ReplenishmentApplyVO;
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
@Schema(description = "补仓申请状态流详情")
public class ReplenishmentApplyFlowDetailVO {

    private ReplenishmentApplyVO apply;
    private Long applicantUserId;
    private String applicantNickname;
    private Long currentHandlerUserId;
    private ReplenishmentStatusEnum currentStatus;
    private Boolean returnedToApplicant;
    private Boolean everRejected;
    private Integer submitVersion;
    private String lastRejectReason;
    private List<BusinessFlowNodeVO> nodes;
}

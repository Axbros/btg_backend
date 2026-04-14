package com.btg.commission.vo.flow;

import com.btg.commission.entity.ProfitReport;
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
@Schema(description = "利润上报链路详情")
public class ProfitReportFlowDetailVO {

    private ProfitReport report;
    private Long applicantUserId;
    private String applicantNickname;
    private String applicantMobile;
    private Long currentHandlerUserId;
    private String currentHandlerNickname;
    private ProfitReportStatus currentStatus;
    private Boolean returnedToApplicant;
    private Integer submitVersion;
    private String lastRejectReason;
    private List<BusinessFlowNodeVO> nodes;
}

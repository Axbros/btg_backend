package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "当前用户可归仓的补仓单（SUCCESS、剩余应还大于 0、已指定资方执行人）")
public class RepayableReplenishmentVO {

    private Long id;

    @Schema(description = "补仓单号 apply_no")
    private String applyNo;

    private BigDecimal approvedAmount;
    private BigDecimal repaidAmount;
    private BigDecimal pendingRepayAmount;
    private BigDecimal remainingAmount;

    @Schema(description = "当前补仓执行人 btg_user.id")
    private Long assignedCapitalUserId;

    @Schema(description = "当前补仓执行人昵称")
    private String assignedCapitalUserName;

    @Schema(description = "补仓执行人 btg_user_profile.exchange_uid；无资料或未填时为 null")
    private String assignedCapitalExchangeUid;

    @Schema(description = "补仓执行方收款 UID")
    private String capitalReceiverUid;

    @Schema(description = "补仓主单状态码，与 ReplenishmentStatusEnum 一致")
    private Integer status;

    @Schema(description = "补仓终审时间")
    private LocalDateTime auditTime;

    private String transferScreenshotUrl;
    private String transferRemark;
}

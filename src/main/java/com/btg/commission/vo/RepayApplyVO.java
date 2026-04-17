package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RepayApplyVO {

    private Long id;
    private String repayNo;
    private Long replenishApplyId;
    private Long userId;

    @Schema(description = "申请人 btg_user.nickname")
    private String nickname;

    @Schema(description = "申请人 btg_user.mobile")
    private String mobile;

    @Schema(description = "关联补仓单 replenishApplyId 对应完整信息（与补仓模块 VO 一致）")
    private ReplenishmentApplyVO replenishmentApply;

    private BigDecimal repayAmount;
    private String repayScreenshotUrl;
    private Integer status;
    private LocalDateTime submitTime;
    private LocalDateTime auditTime;
    private Long auditBy;
    private String auditRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Schema(description = "补仓执行方 / 归仓审核人")
    private Long capitalUserId;
    @Schema(description = "补仓执行方昵称")
    private String capitalUserName;
    @Schema(description = "补仓执行方收款 UID 快照")
    private String capitalReceiverUid;

    private Long currentHandlerUserId;
    @Schema(description = "当前处理人昵称")
    private String currentHandlerUserName;

    @Schema(description = "提交次数")
    private Integer submitVersion;

    @Schema(description = "最近一次拒绝原因")
    private String lastRejectReason;
}

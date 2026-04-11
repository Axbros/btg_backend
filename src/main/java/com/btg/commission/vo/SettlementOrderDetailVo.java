package com.btg.commission.vo;

import com.btg.commission.enums.SettlementOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SettlementOrderDetailVo {

    private Long id;
    private Long rootReportId;
    private Long fromUserId;
    private Long toUserId;
    private Integer levelNo;
    private BigDecimal payAmount;
    private SettlementOrderStatus status;

    /** 本结算单付款人提交的划转截图 */
    private String transferScreenshotUrl;

    /** 关联利润单附件：利润截图（PROFIT）；上缴截图见本单 {@code transferScreenshotUrl} */
    @Schema(description = "关联利润单：利润截图 URL")
    private String profitScreenshotUrl;
    private LocalDateTime submitTime;
    private LocalDateTime auditTime;
    private Long auditBy;
    private String auditRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Schema(description = "关联利润单编号")
    private String reportNo;

    private String fromUserNickname;
    private String fromUserMobile;
    private String toUserNickname;
    private String toUserMobile;
}

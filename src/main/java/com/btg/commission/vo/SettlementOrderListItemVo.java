package com.btg.commission.vo;

import com.btg.commission.enums.SettlementOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "结算单列表项：与结算单表字段一致，并含付款人昵称、手机号")
public class SettlementOrderListItemVo {

    private Long id;
    private Long rootReportId;
    private String reportUserNickname;
//    private Long fromUserId;
//    private Long toUserId;
//    private Integer levelNo;
    private BigDecimal payAmount;
    private SettlementOrderStatus status;
//    private String transferScreenshotUrl;
//    private LocalDateTime submitTime;
//    private LocalDateTime auditTime;
//    private Long auditBy;
//    private String auditRemark;
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
//    private LocalDateTime deletedAt;

//    @Schema(description = "付款人 btg_user.nickname，trim 后非空；否则 null")
//    private String fromUserNickname;

//    @Schema(description = "付款人 btg_user.mobile")
//    private String fromUserMobile;
}

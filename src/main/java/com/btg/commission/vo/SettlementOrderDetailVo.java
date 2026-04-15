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

    @Schema(description = "关联利润单申报人用户 id")
    private Long reportUserId;
    @Schema(description = "关联利润单申报人昵称")
    private String reportUserNickname;
    @Schema(description = "关联利润单申报人手机号")
    private String reportUserMobile;
    @Schema(description = "关联利润单上报利润金额")
    private BigDecimal profitAmount;
    /**
     * 收款上级（to）对付款下级（from）配置的子级分润绝对比例（相对总利润），与 {@code btg_user_profit_config.child_profit_ratio} 一致。
     */
    @Schema(description = "上级对下级的分配利润比例（子级分润绝对比例）")
    private BigDecimal parentToChildProfitRatio;

    private String fromUserNickname;
    private String fromUserMobile;
    private String toUserNickname;
    private String toUserMobile;

    /** 收款人 {@code btg_user_profile.exchange_uid}；无资料或未填时为 null */
    @Schema(description = "收款人资料中的交易所 UID")
    private String toUserExchangeUid;
}

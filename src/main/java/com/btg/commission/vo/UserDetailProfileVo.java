package com.btg.commission.vo;

import com.btg.commission.enums.QualificationStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code GET /user/{id}} 中资料体：与 {@code btg_user_profile} 业务字段一致；
 * 交易账户密码仅根用户可见明文，其余查看者该字段为 null。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailProfileVo {

    private Long id;
    private Long userId;

    private String realName;
    private String idCardNo;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String facePhotoUrl;
    private String serverName;
    private String tradingAccountId;

    @Schema(description = "交易账户密码（库内明文）；仅查看者为系统根用户时返回，否则为 null")
    private String tradingAccountPassword;

    private String exchangeUid;
    private String walletName;
    private String walletAddress;
    private BigDecimal principalAmount;

    private QualificationStatusEnum qualificationStatus;
    private LocalDateTime qualificationAuditTime;
    private Long qualificationAuditBy;
    private String qualificationAuditRemark;
    private Integer qualificationSubmitCount;
    private LocalDateTime qualificationLastSubmitTime;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

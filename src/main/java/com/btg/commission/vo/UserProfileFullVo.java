package com.btg.commission.vo;

import com.btg.commission.enums.KycStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code btg_user_profile} 全字段视图（按 ID 查用户时使用）。
 */
@Data
@Builder
public class UserProfileFullVo {

    private Long id;
    private Long userId;
    private String realName;
    private String idCardNo;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String facePhotoUrl;
    private KycStatus kycStatus;
    private LocalDateTime kycAuditTime;
    private String kycAuditRemark;
    private String serverName;
    private String tradingAccountId;
    private String tradingAccountPassword;
    private String exchangeUid;
    private BigDecimal principalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

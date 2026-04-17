package com.btg.commission.vo;

import com.btg.commission.enums.QualificationStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileVo {

    private String nickname;
    private String realName;
    private String idCardNo;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String facePhotoUrl;
    private String serverName;
    private String tradingAccountId;
    private String exchangeUid;
    private String walletName;
    private String walletAddress;
    private BigDecimal principalAmount;

    private QualificationStatusEnum qualificationStatus;
    private LocalDateTime qualificationAuditTime;
    private String qualificationAuditRemark;
    private Integer qualificationSubmitCount;
    private LocalDateTime qualificationLastSubmitTime;
    /** 仅本人资料 VO 中语义明确：资格被拒绝时可调用重提接口 */
    private Boolean canResubmitQualification;
}

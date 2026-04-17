package com.btg.commission.vo;

import com.btg.commission.enums.QualificationStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "待系统管理员资格审核的用户行")
public class PendingQualificationUserVO {

    private Long userId;
    private String mobile;
    private String nickname;
    private String realName;
    private String serverName;
    private String tradingAccountId;
    private String exchangeUid;
    private BigDecimal principalAmount;
    private QualificationStatusEnum qualificationStatus;
    private LocalDateTime createdAt;
}

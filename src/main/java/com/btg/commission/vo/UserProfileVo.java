package com.btg.commission.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

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
}

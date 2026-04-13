package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UserProfileVo {

    @Schema(description = "Bitget API 是否三者均已配置（access / secret / passphrase）")
    private Boolean bitgetConfigured;

    @Schema(description = "Bitget access key 掩码展示")
    private String accessKeyMasked;

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

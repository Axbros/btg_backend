package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Bitget 资产汇总（独立接口返回）")
public class BitgetAssetSummaryVO {

    private Boolean success;
    private String message;

    @Schema(description = "接口来源标识：ALL_ACCOUNT_BALANCE（GET /api/v2/account/all-account-balance）")
    private String productType;

    private List<BitgetAssetAccountVO> accounts;
    /** 各账户类型 usdtBalance 之和（字符串小数） */
    private String totalUsdtBalance;
    private String lastSyncTime;
}

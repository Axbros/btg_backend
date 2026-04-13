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

    @Schema(description = "本次查询使用的 productType，如 USDT-FUTURES")
    private String productType;

    private List<BitgetAssetAccountVO> accounts;
    /** 各账户 usdtEquity 之和（字符串小数） */
    private String totalUsdtBalance;
    private String lastSyncTime;
}

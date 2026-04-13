package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Bitget 合约账户信息（mix/account/accounts 单条 data）")
public class BitgetAssetAccountVO {

    @Schema(description = "保证金币种 marginCoin，如 USDT")
    private String coin;

    @Schema(description = "产品类型，与请求 productType 一致，如 USDT-FUTURES")
    private String accountType;

    /** 对应 Bitget usdtEquity（折算 USDT 账户权益） */
    private String usdtBalance;

    private String usdtAvailable;
    private String usdtFrozen;
}

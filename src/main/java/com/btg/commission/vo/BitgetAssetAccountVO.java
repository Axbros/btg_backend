package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Bitget 全账户余额 data 中单条（accountType + usdtBalance）")
public class BitgetAssetAccountVO {

    @Schema(description = "与 accountType 相同或为空；全账户余额接口以 accountType 为主")
    private String coin;

    @Schema(description = "账户类型：spot、futures、funding、earn、bots、margin 等")
    private String accountType;

    /** Bitget 返回的该账户类型 USDT 余额 */
    private String usdtBalance;

    @Schema(description = "全账户余额接口通常无此字段，可能为 null")
    private String usdtAvailable;

    @Schema(description = "全账户余额接口通常无此字段，可能为 null")
    private String usdtFrozen;
}

package com.btg.commission.vo;

import com.btg.commission.entity.UserProfitConfig;
import com.btg.commission.enums.CommissionModeEnum;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 当前用户在上级处的分润配置，并附带直属上级资料中的 {@code exchange_uid}。
 * 序列化时与 {@link UserProfitConfig} 字段平级，另含 {@code parentExchangeUid}、{@code commissionModeDesc}。
 */
@Data
public class SelfUnderParentProfitConfigVo {

    @JsonUnwrapped
    private UserProfitConfig config;

    @Schema(description = "直属上级 btg_user_profile.exchange_uid；无资料或未填时为 null")
    private String parentExchangeUid;

    @Schema(description = "当前 commission_mode 中文：兜底 / 不兜底；未知模式时为 null")
    private String commissionModeDesc;

    public SelfUnderParentProfitConfigVo(UserProfitConfig config, String parentExchangeUid) {
        this(config, parentExchangeUid, CommissionModeEnum.descriptionOrNull(
                config == null ? null : config.getCommissionMode()));
    }

    public SelfUnderParentProfitConfigVo(UserProfitConfig config, String parentExchangeUid, String commissionModeDesc) {
        this.config = config;
        this.parentExchangeUid = parentExchangeUid;
        this.commissionModeDesc = commissionModeDesc;
    }
}

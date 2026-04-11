package com.btg.commission.vo;

import com.btg.commission.entity.UserProfitConfig;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前用户在上级处的分润配置，并附带直属上级资料中的 {@code exchange_uid}。
 * 序列化时与 {@link UserProfitConfig} 字段平级，另含 {@code parentExchangeUid}。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelfUnderParentProfitConfigVo {

    @JsonUnwrapped
    private UserProfitConfig config;

    @Schema(description = "直属上级 btg_user_profile.exchange_uid；无资料或未填时为 null")
    private String parentExchangeUid;
}

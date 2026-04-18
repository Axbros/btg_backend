package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UserDetailVo {

    /** 与 {@code btg_user} 一致的可对外字段（无密码） */
    private UserDetailUserVo user;

    /** 与 {@code btg_user_profile} 业务字段一致；交易账户密码仅根用户可见 */
    private UserDetailProfileVo profile;

    /**
     * 当前登录用户视角下，该用户所在直属分支的「子级总利润占比」（0～1，与分润配置一致）；
     * 非本人下级或未配置时为 null。
     */
    @Schema(description = "当前登录用户对目标所在分支的子级总利润占比（ACTIVE 配置）；非下级或未配置为 null")
    private BigDecimal childLineProfitRatio;

    /**
     * 当前登录用户为该分支上级时，给直属下级设置「子级总利润占比」允许的最大值（0～1，与分润配置接口上限一致）。
     * 业务上即「可分配的最大利润」对应的比例上限；非本人下级链或本人无上級可分比例时为 null。
     */
    @Schema(description = "调整子级总利润占比时，当前用户可配置的上限（0～1）；与 parentAssignableRatio 口径一致")
    private BigDecimal maxAssignableChildProfitRatio;
}

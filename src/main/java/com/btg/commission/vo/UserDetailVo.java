package com.btg.commission.vo;

import com.btg.commission.entity.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UserDetailVo {

    /** 与 {@code btg_user} 一致的可对外字段（无密码） */
    private UserDetailUserVo user;

    /** 与 {@code btg_user_profile} 行一致；无资料行时为 null */
    private UserProfile profile;

    /**
     * 当前登录用户视角下，该用户所在直属分支的「子级总利润占比」（0～1，与分润配置一致）；
     * 非本人下级或未配置时为 null。
     */
    @Schema(description = "当前登录用户对目标所在分支的子级总利润占比（ACTIVE 配置）；非下级或未配置为 null")
    private BigDecimal childLineProfitRatio;
}

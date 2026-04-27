package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDetailVo {

    /** 与 {@code btg_user} 一致的可对外字段（无密码） */
    private UserDetailUserVo user;

    /** 与 {@code btg_user_profile} 业务字段一致；交易账户密码仅根用户可见 */
    private UserDetailProfileVo profile;

    /**
     * 查看自己：直属上级 → 本人的 ACTIVE 配置；
     * 查看下级链上用户：本人 →「本人到目标路径上直属子」的 ACTIVE 配置；
     * 无上级、无该边或未配置时为 null。
     */
    @Schema(description = "对当前登录用户在该页相关的兜底/不兜底比例与模式")
    private UserDetailViewerProfitConfigVo viewerProfitConfig;
}

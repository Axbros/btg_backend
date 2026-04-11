package com.btg.commission.vo;

import com.btg.commission.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * {@code GET /user/{id}} 中 {@code user} 对象：与 {@code btg_user} 行一致的可对外字段（不含密码哈希）。
 */
@Data
@Builder
public class UserDetailUserVo {

    private Long id;
    private String mobile;
    private UserStatus status;
    private Boolean isRoot;
    private Long referrerUserId;
    private String ancestorPath;
    private String invitationCode;
    private String nickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 直属上级昵称；无上级或上级不存在时为 null */
    private String referrerNickname;
}

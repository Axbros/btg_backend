package com.btg.commission.vo;

import com.btg.commission.enums.KycStatus;
import com.btg.commission.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserMeVo {

    private Long id;
    private String mobile;
    private UserStatus status;
    private Boolean isRoot;
    private Long referrerUserId;
    private String ancestorPath;
    private String invitationCode;
    private String nickname;

    /** 直属上级（推荐人）的昵称；无上级或上级不存在时为 null */
    private String referrerNickname;

    /** 资料表 KYC 状态；无资料行时为 null */
    private KycStatus kycStatus;
}

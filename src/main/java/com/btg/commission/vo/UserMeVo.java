package com.btg.commission.vo;

import com.btg.commission.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "当前登录用户基本信息")
public class UserMeVo {

    private Long id;
    private String mobile;
    private UserStatus status;
    private Boolean isRoot;
    private Long referrerUserId;
    private String ancestorPath;
    private String invitationCode;
    private String nickname;

    /**
     * 直属上级（{@code btg_user.nickname}，trim 后非空）；昵称为空时用上级手机号便于展示。
     * 无上级或上级不存在时为 null；始终参与序列化以便前端识别字段。
     */
    @Schema(description = "直属上级昵称；昵称为空时为上级手机号；无上级为 null")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String referrerNickname;
}

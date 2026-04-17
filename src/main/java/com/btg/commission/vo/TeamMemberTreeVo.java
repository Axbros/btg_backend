package com.btg.commission.vo;

import com.btg.commission.enums.QualificationStatusEnum;
import com.btg.commission.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "团队下级树节点")
public class TeamMemberTreeVo {

    private Long id;

    @Schema(description = "btg_user.nickname，未设置时可能为 null")
    private String nickname;

    @Schema(description = "btg_user.status：-1 待完善；0 待审核；1 正常")
    private UserStatus status;

    @Schema(description = "系统管理员资格审核状态（btg_user_profile）")
    private QualificationStatusEnum qualificationStatus;

    @Schema(description = "系统管理员资格审核时间")
    private LocalDateTime qualificationAuditTime;

    @Schema(description = "系统管理员资格审核备注（可选）")
    private String qualificationAuditRemark;

    @Schema(description = "直属下级子树，无则为空数组")
    private List<TeamMemberTreeVo> children;
}

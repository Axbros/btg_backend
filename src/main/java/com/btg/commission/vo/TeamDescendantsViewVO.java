package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "团队下级树 + 统计（原 /me/team-stats 与 /user/team/descendants 合并）")
public class TeamDescendantsViewVO {

    @Schema(description = "直属下级人数")
    private int directCount;

    @Schema(description = "全部下级（含多代）人数，与树节点总数一致时即为全量去重后人数")
    private int allDescendantCount;

    @Schema(description = "以直属下级为根的多叉树，节点含 id、nickname、status、children。结构与原 /user/team/descendants 仅列表时相同")
    private List<TeamMemberTreeVo> descendants;
}

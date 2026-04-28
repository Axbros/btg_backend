package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分润模式变更审核详情")
public class ProfitConfigModeAuditDetailVO {

    @Schema(description = "待审核配置ID")
    private Long pendingConfigId;

    @Schema(description = "父级用户ID（团队长）")
    private Long parentUserId;

    @Schema(description = "子级用户ID")
    private Long childUserId;

    @Schema(description = "变更前当前生效配置（若缺失则为 null）")
    private UserProfitConfigListItemVO beforeActiveConfig;

    @Schema(description = "变更后待审核配置")
    private UserProfitConfigListItemVO afterPendingConfig;
}

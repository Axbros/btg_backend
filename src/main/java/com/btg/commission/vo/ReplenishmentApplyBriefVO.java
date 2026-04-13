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
@Schema(description = "我的补仓申请列表项（id、单号、状态）")
public class ReplenishmentApplyBriefVO {

    private Long id;

    @Schema(description = "补仓申请单号 apply_no")
    private String applyNo;

    @Schema(description = "状态码：1 待受理；7 待资方上传凭证；8 待终审确认；2 通过；3 拒绝；4 部分归还；5 已结清；6 已关闭")
    private Integer status;
}

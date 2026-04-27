package com.btg.commission.vo;

import com.btg.commission.enums.UserProfitConfigStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 下级分润配置列表项（含模式中文描述） */
@Value
@Builder
public class UserProfitConfigListItemVO {

    Long id;

    Long parentUserId;

    Long childUserId;

    /** 兼容旧字段：与当前生效模式对应比例一致写入 */
    @Schema(description = "子级总利润占比（历史字段；与当前 commission_mode 下对应比例同步）")
    BigDecimal childProfitRatio;

    BigDecimal guaranteeRatio;

    BigDecimal nonGuaranteeRatio;

    String commissionMode;

    @Schema(description = "分润模式中文：兜底 / 不兜底")
    String commissionModeDesc;

    UserProfitConfigStatus status;

    LocalDateTime effectiveTime;

    LocalDateTime expireTime;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}

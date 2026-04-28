package com.btg.commission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btg.commission.enums.ProfitConfigAuditStatus;
import com.btg.commission.enums.UserProfitConfigStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("btg_user_profit_config")
public class UserProfitConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentUserId;
    private Long childUserId;
    private BigDecimal childProfitRatio;
    /** 兜底模式：子级可分总利润比例 */
    private BigDecimal guaranteeRatio;
    /** 不兜底模式：子级可分总利润比例 */
    private BigDecimal nonGuaranteeRatio;
    /** 当前生效分润模式：GUARANTEE / NON_GUARANTEE（由上级设置） */
    private String commissionMode;
    private UserProfitConfigStatus status;
    /** 分润模式切换审核状态：PENDING / APPROVED / REJECTED */
    private ProfitConfigAuditStatus auditStatus;
    /** 审核时间（根用户） */
    private LocalDateTime auditTime;
    /** 审核人（根用户 id） */
    private Long auditorId;
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now()")
    private LocalDateTime deletedAt;
}

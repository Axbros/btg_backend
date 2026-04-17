package com.btg.commission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btg.commission.enums.RepayStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("btg_replenishment_repay_apply")
public class BtgReplenishmentRepayApply {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String repayNo;
    private Long replenishApplyId;
    private Long userId;
    /** 补仓执行方 / 归仓审核人（提交时取自补仓单 assigned_capital_user_id） */
    private Long capitalUserId;
    /** 补仓执行方收款 UID 快照 */
    private String capitalReceiverUid;
    private BigDecimal repayAmount;
    private String repayScreenshotUrl;
    private RepayStatusEnum status;
    private LocalDateTime submitTime;
    private LocalDateTime auditTime;
    private Long auditBy;
    private String auditRemark;
    private Integer submitVersion;
    private Long currentHandlerUserId;
    private String flowStatus;
    private Boolean returnedToUser;
    private String lastRejectReason;
    private LocalDateTime lastRejectTime;
    private Long lastRejectBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now()")
    private LocalDateTime deletedAt;
}

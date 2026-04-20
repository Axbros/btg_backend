package com.btg.commission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btg.commission.enums.ArrivalConfirmStatusEnum;
import com.btg.commission.enums.ReplenishmentStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 补仓申请；{@code transfer_screenshot_url}、{@code transfer_remark} 在资方审核通过时写入。
 */
@Data
@TableName("btg_replenishment_apply")
public class BtgReplenishmentApply {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String applyNo;
    private Long userId;
    /** 提交/重提交时申请人 {@code btg_mt5_account_snapshot} 最新一条主键 */
    private Long mt5SnapshotId;
    private BigDecimal principalAmount;
    private BigDecimal balanceAmount;
    private BigDecimal replenishAmount;
    private String balanceScreenshotUrl;
    /** 资方补仓转账凭证（审核通过时必填落库） */
    private String transferScreenshotUrl;
    /** 资方补仓转账备注 */
    private String transferRemark;
    private ReplenishmentStatusEnum status;
    private BigDecimal approvedAmount;
    private BigDecimal repaidAmount;
    private BigDecimal pendingRepayAmount;
    private BigDecimal remainingAmount;
    private LocalDateTime submitTime;
    private LocalDateTime auditTime;
    private Long auditBy;
    private String auditRemark;
    private Integer submitVersion;
    private Long currentHandlerUserId;
    /** 当前资方执行人（管理员转派后） */
    private Long assignedCapitalUserId;
    private Long assignedBy;
    private LocalDateTime assignedTime;
    private String assignRemark;
    private LocalDateTime capitalSubmitTime;
    private String capitalSubmitRemark;
    private String capitalReceiverUid;
    private ArrivalConfirmStatusEnum arrivalConfirmStatus;
    private LocalDateTime arrivalConfirmTime;
    private Long arrivalConfirmBy;
    private String arrivalConfirmRemark;
    private String flowStatus;
    private Boolean returnedToUser;
    private String lastRejectReason;
    private LocalDateTime lastRejectTime;
    private Long lastRejectBy;

    /** 资方受理时间 */
    private LocalDateTime acceptedAt;
    /** 资方受理人 */
    private Long acceptedBy;
    /** 历史字段，新流程不再写入；资方凭证见 {@link #transferScreenshotUrl} */
    private String supplementScreenshotUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now()")
    private LocalDateTime deletedAt;
}

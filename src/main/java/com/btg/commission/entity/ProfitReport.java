package com.btg.commission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btg.commission.enums.ProfitReportStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("btg_profit_report")
public class ProfitReport {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String reportNo;
    private Long reportUserId;
    private Long directParentUserId;
    private BigDecimal profitAmount;
    private ProfitReportStatus status;
    private LocalDateTime submitTime;
    private LocalDateTime auditTime;
    private Long auditBy;
    private String auditRemark;
    private Integer submitVersion;
    private Long currentHandlerUserId;
    private String flowStatus;
    private String currentStepStatus;
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

package com.btg.commission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("btg_user_account_summary")
public class UserAccountSummary {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private BigDecimal totalProfitAmount;
    private BigDecimal totalCommissionOutAmount;
    private BigDecimal totalCommissionInAmount;
    private BigDecimal pendingCommissionOutAmount;
    private BigDecimal pendingCommissionInAmount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

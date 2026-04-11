package com.btg.commission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btg.commission.enums.BindingStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("btg_user_commission_binding")
public class UserCommissionBinding {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long referrerUserId;
    private Long childUserId;
    private Long strategyId;
    private BigDecimal commissionRateSnapshot;
    private BindingStatus status;
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

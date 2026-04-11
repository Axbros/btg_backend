package com.btg.commission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btg.commission.enums.CommissionRecordStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("btg_commission_record")
public class CommissionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long profitRecordId;
    private Long fromUserId;
    private Long toUserId;
    private Long strategyId;
    private BigDecimal commissionRate;
    private BigDecimal profitAmount;
    private BigDecimal commissionAmount;
    private CommissionRecordStatus status;
    private LocalDateTime confirmedTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

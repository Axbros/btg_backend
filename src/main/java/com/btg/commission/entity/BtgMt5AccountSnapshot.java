package com.btg.commission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("btg_mt5_account_snapshot")
public class BtgMt5AccountSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String accountId;
    private String serverName;
    private BigDecimal balance;
    private BigDecimal equity;
    private BigDecimal lastBalance;
    private BigDecimal lastEquity;
    private BigDecimal profit;
    private BigDecimal marginAmount;
    private BigDecimal freeMargin;
    private BigDecimal marginLevel;
    private String source;
    private LocalDateTime snapshotTime;
    /** 原始上报 JSON 字符串（列类型可为 JSON，驱动映射为 String） */
    private String rawPayload;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now()")
    private LocalDateTime deletedAt;
}

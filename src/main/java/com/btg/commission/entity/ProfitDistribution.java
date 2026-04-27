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
@TableName("btg_profit_distribution")
public class ProfitDistribution {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reportId;
    /** 本次分润明细使用的分润模式快照（与对应利润单一致） */
    private String commissionMode;
    private Long beneficiaryUserId;
    private Integer levelNo;
    /** 切片上界比例快照（根为 1） */
    private BigDecimal upperRatio;
    /** 切片下界比例快照（最底层为 0） */
    private BigDecimal lowerRatio;
    private BigDecimal incomeAmount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now()")
    private LocalDateTime deletedAt;
}

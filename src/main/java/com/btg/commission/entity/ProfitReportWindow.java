package com.btg.commission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("btg_profit_report_window")
public class ProfitReportWindow {

    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate businessDate;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private Long openedByUserId;
    private Long closedByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now()")
    private LocalDateTime deletedAt;
}

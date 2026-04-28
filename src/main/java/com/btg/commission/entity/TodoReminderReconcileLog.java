package com.btg.commission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("btg_todo_reminder_reconcile_log")
public class TodoReminderReconcileLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String metricKey;
    private Integer legacyCount;
    private Integer reminderCount;
    private Integer diffCount;
    private LocalDateTime comparedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

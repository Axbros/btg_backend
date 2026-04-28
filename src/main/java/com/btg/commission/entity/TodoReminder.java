package com.btg.commission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btg.commission.enums.ReminderStateEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("btg_todo_reminder")
public class TodoReminder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String taskType;
    private Long relatedId;
    private String status;
    /** 统一待办类型（可替代 task_type 语义） */
    private String todoType;
    /** 业务域，例如 settlement/profit_report/replenishment/repay/qualification */
    private String businessType;
    /** 业务主键 */
    private Long businessId;
    /** 待办归属用户 */
    private Long ownerUserId;
    /** OPEN / DONE / CANCELLED */
    private ReminderStateEnum reminderState;
    /** 源业务状态快照（可选） */
    private String sourceStatus;
    /** 源业务最近更新时间（可选） */
    private LocalDateTime sourceUpdatedAt;
    /** 幂等去重键：todoType:businessType:businessId:ownerUserId */
    private String dedupeKey;
    /** 关闭时间（DONE/CANCELLED） */
    private LocalDateTime resolvedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

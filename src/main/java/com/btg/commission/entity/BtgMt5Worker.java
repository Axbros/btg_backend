package com.btg.commission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("btg_mt5_worker")
public class BtgMt5Worker {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String workerCode;
    private String workerName;
    /** 0 离线 1 在线 2 禁用 */
    private Integer status;
    private Integer maxAccounts;
    private Integer currentAccounts;
    private LocalDateTime lastHeartbeatTime;
    private Integer heartbeatExpireSeconds;
    private String version;
    private String hostName;
    private String ipAddress;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now()")
    private LocalDateTime deletedAt;

    /** 仅查询填充：当前有效已分配账号数 */
    @TableField(exist = false)
    private Integer liveAssignedAccountCount;

    public boolean isDisabled() {
        return status != null && status == 2;
    }
}

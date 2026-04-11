package com.btg.commission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btg.commission.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("btg_user")
public class BtgUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String mobile;
    private String passwordHash;
    private UserStatus status;
    private Boolean isRoot;
    private Long referrerUserId;
    private String ancestorPath;
    private String invitationCode;
    private String nickname;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now()")
    private LocalDateTime deletedAt;
}

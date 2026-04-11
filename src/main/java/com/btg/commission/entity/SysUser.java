package com.btg.commission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btg.commission.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("btg_sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String mobile;
    private String passwordHash;
    private String loginSalt;
    private UserStatus status;
    private Boolean isRoot;
    private Long referrerUserId;
    private String ancestorPath;
    private String invitationCode;
    private String nickname;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

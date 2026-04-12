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
@TableName("btg_user_profile")
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String realName;
    private String idCardNo;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String facePhotoUrl;
    private String serverName;
    private String tradingAccountId;
    private String tradingAccountPassword;
    private String exchangeUid;
    /** 券商名称 */
    private String walletName;
    /** 钱包地址 */
    private String walletAddress;
    private BigDecimal principalAmount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now()")
    private LocalDateTime deletedAt;
}

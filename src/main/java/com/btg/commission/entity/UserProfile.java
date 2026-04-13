package com.btg.commission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    /**
     * Bitget API Key（明文落库；勿写入日志、勿通过 JSON 对外暴露）。
     */
    @JsonIgnore
    private String bitgetAccessKey;

    @JsonIgnore
    private String bitgetSecretKey;

    @JsonIgnore
    private String bitgetPassphrase;

    /** 仅接口展示：三者均已配置时为 true；非表字段 */
    @TableField(exist = false)
    private Boolean bitgetConfigured;

    /** 仅接口展示：accessKey 掩码；非表字段 */
    @TableField(exist = false)
    private String accessKeyMasked;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now()")
    private LocalDateTime deletedAt;
}

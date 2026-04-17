package com.btg.commission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btg.commission.enums.QualificationStatusEnum;
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
    /** 不参与 JSON 序列化（用户详情等接口勿泄露交易账户密码） */
    @JsonIgnore
    private String tradingAccountPassword;
    private String exchangeUid;
    /** 券商名称 */
    private String walletName;
    /** 钱包地址 */
    private String walletAddress;
    private BigDecimal principalAmount;

    private QualificationStatusEnum qualificationStatus;
    private LocalDateTime qualificationAuditTime;
    private Long qualificationAuditBy;
    private String qualificationAuditRemark;
    /** 资格审核提交次数（注册为 1，每次用户重提 +1） */
    private Integer qualificationSubmitCount;
    /** 最近一次用户提交/重提资格审核时间 */
    private LocalDateTime qualificationLastSubmitTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now()")
    private LocalDateTime deletedAt;
}

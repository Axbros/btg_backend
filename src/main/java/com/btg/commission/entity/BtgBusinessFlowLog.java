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
@TableName("btg_business_flow_log")
public class BtgBusinessFlowLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 与 {@link com.btg.commission.enums.BusinessFlowType#name()} 一致 */
    private String businessType;
    private Long businessId;
    private Long rootBusinessId;
    private Long nodeUserId;
    /** {@link com.btg.commission.enums.FlowNodeRole#name()} */
    private String nodeRole;
    /** {@link com.btg.commission.enums.FlowAction#name()} */
    private String action;
    private String statusAfterAction;
    private Integer versionNo;
    private String remark;
    private Long operatorUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now()")
    private LocalDateTime deletedAt;
}

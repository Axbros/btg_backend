package com.btg.commission.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UserDetailVo {

    private UserMeVo user;

    /** 无资料行时为 null */
    private UserProfileFullVo profile;

    /**
     * 与直属上级之间的有效分佣绑定：策略 ID；无绑定或未匹配直属关系时为 null。
     */
//    private Long strategyId;

    /** 策略名称；无绑定或策略已删时为 null */
    private String strategyName;

    /**
     * 上述绑定中的分佣比例快照（与策略绑定当时一致）；无绑定时为 null。
     */
    private BigDecimal commissionRate;
}

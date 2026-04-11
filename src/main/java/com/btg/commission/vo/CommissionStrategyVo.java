package com.btg.commission.vo;

import com.btg.commission.enums.StrategyStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CommissionStrategyVo {

    private Long id;
    private String strategyCode;
    private String strategyName;
    private BigDecimal commissionRate;
    private String description;
    private StrategyStatus status;
    private Integer sortNo;
    private LocalDateTime createdAt;
}

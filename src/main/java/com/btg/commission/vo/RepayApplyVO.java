package com.btg.commission.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RepayApplyVO {

    private Long id;
    private String repayNo;
    private Long replenishApplyId;
    private Long userId;
    private BigDecimal repayAmount;
    private String repayScreenshotUrl;
    private Integer status;
    private LocalDateTime submitTime;
    private LocalDateTime auditTime;
    private Long auditBy;
    private String auditRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

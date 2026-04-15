package com.btg.commission.vo.flow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 总利润切片模型中的一层（与 {@code btg_profit_distribution.level_no} 对齐，根为 0，申报人为最大 level）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分润切片层级：比例、本层分润、上缴父级金额、关联结算状态与当前处理人")
public class ProfitFlowLayerVO {

    private Integer levelNo;
    private Long userId;
    private String userName;
    private Long parentUserId;
    private String parentUserName;
    private Long childUserId;
    private String childUserName;
    /** 切片上界比例（相对总利润），根为 1 */
    private BigDecimal upperRatio;
    /** 切片下界比例（相对总利润），最底层为 0 */
    private BigDecimal lowerRatio;
    /** 本层分润金额，来自 btg_profit_distribution */
    private BigDecimal incomeAmount;
    /**
     * 本层向父级上缴金额：与 {@code btg_settlement_order} 中 from=本层用户、to=父级的边一致；根层无上缴则为 null。
     */
    private BigDecimal payAmountToParent;
    /** 关联结算边状态名，如 INIT / PENDING_SUBMIT / PENDING_REVIEW / APPROVED / REJECTED；根层无结算边时为 null */
    private String settlementStatus;
    private Long currentHandlerUserId;
    private String currentHandlerUserName;
    private String remark;
    private LocalDateTime operateTime;
    /** 当前全局流转是否卡在本层（待本层或本层关联结算动作） */
    private Boolean currentNode;
    /** 本层金额/比例是否因权限被裁剪（前端可配合展示） */
    private Boolean financialsMasked;
}

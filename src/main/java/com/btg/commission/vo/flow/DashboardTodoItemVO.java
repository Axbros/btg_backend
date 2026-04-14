package com.btg.commission.vo.flow;

import com.btg.commission.enums.DashboardTodoType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "首页待办/待处理统一列表项")
public class DashboardTodoItemVO {

    private DashboardTodoType todoType;
    private Long businessId;
    private String title;
    /** 主单状态枚举名或业务状态文案键 */
    private String currentStatus;
    private Long currentHandlerUserId;
    private String lastRejectReason;
    private LocalDateTime latestOperateTime;
    /** 前端路由提示，如 profit-report / replenishment / repay */
    private String routeHint;
    /** 按钮或下一步动作提示 */
    private String actionHint;
}

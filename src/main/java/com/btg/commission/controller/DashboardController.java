package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.DashboardService;
import com.btg.commission.vo.PendingSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "工作台")
@RestController
@RequestMapping("${btg.api.base-path}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "待办数量汇总", description = "结算待审、利润待审、本人待支付给上级的结算单数；根用户另含补仓/归仓待审。不含金额。")
    @GetMapping("/pending-summary")
    public ApiResult<PendingSummaryVO> pendingSummary() {
        return ApiResult.ok(dashboardService.getPendingSummary(SecurityUtils.requireUserId()));
    }
}

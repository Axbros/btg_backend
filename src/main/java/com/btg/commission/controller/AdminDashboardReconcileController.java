package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.ReminderReconcileService;
import com.btg.commission.vo.ReminderReconcileResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理-工作台对账")
@RestController
@RequestMapping("${btg.api.base-path}/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardReconcileController {

    private final ReminderReconcileService reminderReconcileService;

    @Operation(summary = "pending-summary 对账查询（旧口径 vs reminder）")
    @GetMapping("/pending-summary-reconcile")
    public ApiResult<ReminderReconcileResultVO> reconcilePendingSummary(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean persistLog) {
        SecurityUtils.requireRootUser();
        return ApiResult.ok(reminderReconcileService.reconcilePendingSummaryForUser(userId, persistLog));
    }

    @Operation(summary = "pending-summary 批量对账（最新 N 个用户）")
    @GetMapping("/pending-summary-reconcile/batch")
    public ApiResult<List<ReminderReconcileResultVO>> reconcilePendingSummaryBatch(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean persistLog) {
        SecurityUtils.requireRootUser();
        return ApiResult.ok(reminderReconcileService.reconcileLatestUsers(size, persistLog));
    }
}

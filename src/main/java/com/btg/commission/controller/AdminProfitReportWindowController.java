package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.ProfitReportWindowService;
import com.btg.commission.vo.ProfitReportWindowTodayVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 根用户按上海自然日控制利润上报窗口：{@code btg_profit_report_window} 每个 {@code business_date} 一行，历史保留；
 * 开始结算后成员可上报，结束结算后上报被拒绝并提示不在规定时间。
 * 与 {@link com.btg.commission.config.SecurityConfig} 中 {@code /admin/**} 的 ADMIN 角色及 {@link SecurityUtils#requireRootUser()}、
 * Service 内 {@code btg_user.is_root} 校验一致。
 */
@Tag(name = "管理-利润上报时间窗口")
@RestController
@RequestMapping("${btg.api.base-path}/admin/profit-report-window")
@RequiredArgsConstructor
public class AdminProfitReportWindowController {

    private final ProfitReportWindowService profitReportWindowService;

    @Operation(
            summary = "当日结算窗口状态",
            description = "按 Asia/Shanghai 的 business_date=今日 查询。openedAt 非空则勿再调 start；closedAt 非空则勿再调 stop。"
                    + "acceptingProfitReport=true 时启用「结束结算」；仅当 openedAt 为空时可调 start（通常即无当日记录）。")
    @GetMapping("/today")
    public ApiResult<ProfitReportWindowTodayVO> today() {
        Long operatorId = SecurityUtils.requireRootUser().getUserId();
        return ApiResult.ok(profitReportWindowService.getTodayWindow(operatorId));
    }

    @Operation(
            summary = "开始结算",
            description = "为当日插入窗口行并写入 openedAt；当日 openedAt 已有则 409，不可重复开始、也不可结束后再开。")
    @PostMapping("/start")
    public ApiResult<Void> start() {
        Long operatorId = SecurityUtils.requireRootUser().getUserId();
        profitReportWindowService.start(operatorId);
        return ApiResult.ok();
    }

    @Operation(
            summary = "结束结算",
            description = "写入 closedAt；未开始不可调；当日 closedAt 已有则 409。结束后成员上报提示不在规定时间。")
    @PostMapping("/stop")
    public ApiResult<Void> stop() {
        Long operatorId = SecurityUtils.requireRootUser().getUserId();
        profitReportWindowService.stop(operatorId);
        return ApiResult.ok();
    }
}

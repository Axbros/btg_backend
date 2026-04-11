package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.AccountOverviewService;
import com.btg.commission.service.TeamStatsService;
import com.btg.commission.service.UserCommissionBindingService;
import com.btg.commission.vo.AccountSummaryVo;
import com.btg.commission.vo.MyActiveCommissionStrategyVo;
import com.btg.commission.vo.TeamStatsVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "我的概览")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeOverviewController {

    private final AccountOverviewService accountOverviewService;
    private final TeamStatsService teamStatsService;
    private final UserCommissionBindingService userCommissionBindingService;

    @GetMapping("/account-summary")
    public ApiResult<AccountSummaryVo> accountSummary() {
        return ApiResult.ok(accountOverviewService.summary(SecurityUtils.requireUserId()));
    }

    @GetMapping("/team-stats")
    public ApiResult<TeamStatsVo> teamStats() {
        return ApiResult.ok(teamStatsService.stats(SecurityUtils.requireUserId()));
    }

    @Operation(summary = "我的分佣策略", description = "可选 profitAmount：previewCommissionAmount/previewTransferAmount=盈利×(1−比例)（分给上级）；previewNetAmount=盈利×比例（本人自留）；与申报单 commission_amount/net_amount 一致。")
    @GetMapping("/commission-strategy")
    public ApiResult<MyActiveCommissionStrategyVo> myCommissionStrategy(
            @Parameter(description = "可选：申报盈利金额，用于预览 previewCommissionAmount / previewTransferAmount")
            @RequestParam(required = false) BigDecimal profitAmount) {
        return ApiResult.ok(userCommissionBindingService.getMyActiveCommissionStrategy(SecurityUtils.requireUserId(), profitAmount));
    }
}

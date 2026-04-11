package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.AccountOverviewService;
import com.btg.commission.service.TeamStatsService;
import com.btg.commission.service.UserService;
import com.btg.commission.vo.AccountSummaryVo;
import com.btg.commission.vo.TeamStatsVo;
import com.btg.commission.vo.UserMeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "我的概览")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeOverviewController {

    private final AccountOverviewService accountOverviewService;
    private final TeamStatsService teamStatsService;
    private final UserService userService;

    @Operation(summary = "当前登录用户", description = "含 referrerNickname、btg_user_profile 资料摘要（UserMeVo.profile）")
    @GetMapping
    public ApiResult<UserMeVo> me() {
        UserMeVo vo = userService.me(SecurityUtils.requireUserId());
        if (vo == null) {
            throw new BizException(ResultCode.NOT_FOUND, "user not found");
        }
        return ApiResult.ok(vo);
    }

    @GetMapping("/account-summary")
    public ApiResult<AccountSummaryVo> accountSummary() {
        return ApiResult.ok(accountOverviewService.summary(SecurityUtils.requireUserId()));
    }

    @GetMapping("/team-stats")
    public ApiResult<TeamStatsVo> teamStats() {
        return ApiResult.ok(teamStatsService.stats(SecurityUtils.requireUserId()));
    }
}

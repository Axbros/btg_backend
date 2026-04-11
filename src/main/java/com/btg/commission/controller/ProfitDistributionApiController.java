package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.entity.ProfitDistribution;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.ProfitReportService;
import com.btg.commission.vo.ProfitDistributionVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "分润明细")
@RestController
@RequestMapping("/api/v1/profit-distributions")
@RequiredArgsConstructor
public class ProfitDistributionApiController {

    private final ProfitReportService profitReportService;

    @Operation(summary = "按利润单查看分润明细", description = "beneficiaryDisplayName（昵称或手机号）仅根用户返回；其余用户为 null")
    @GetMapping("/report/{reportId}")
    public ApiResult<List<ProfitDistributionVo>> byReport(@PathVariable Long reportId) {
        return ApiResult.ok(profitReportService.listDistributionsForReport(SecurityUtils.requireUserId(), reportId));
    }

    @GetMapping("/mine")
    public ApiResult<List<ProfitDistribution>> mine() {
        return ApiResult.ok(profitReportService.listDistributionsForMine(SecurityUtils.requireUserId()));
    }
}

package com.btg.commission.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.v1.ProfitReportRejectRequest;
import com.btg.commission.dto.v1.ProfitReportResubmitRequest;
import com.btg.commission.dto.v1.ProfitReportSubmitRequest;
import com.btg.commission.entity.ProfitReport;
import com.btg.commission.vo.flow.ProfitReportFlowDetailVO;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.ProfitReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "利润上报")
@RestController
@RequestMapping("${btg.api.base-path}/profit-reports")
@RequiredArgsConstructor
public class ProfitReportApiController {

    private final ProfitReportService profitReportService;

    @PostMapping
    public ApiResult<Long> submit(@Valid @RequestBody ProfitReportSubmitRequest req) {
        Long id = profitReportService.submit(
                SecurityUtils.requireUserId(),
                req.getProfitAmount(),
                req.getProfitScreenshotUrl(),
                req.getTransferToParentScreenshotUrl());
        return ApiResult.ok(id);
    }

    @GetMapping("/mine")
    public ApiResult<Page<ProfitReport>> mine(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(profitReportService.pageMine(SecurityUtils.requireUserId(), page, size));
    }

    @GetMapping("/pending-review")
    public ApiResult<ProfitReportService.PendingReviewBundle> pendingReview() {
        return ApiResult.ok(profitReportService.pendingReviewForDirectSupervisor(SecurityUtils.requireUserId()));
    }

    @PostMapping("/{id}/reject")
    public ApiResult<Void> reject(@PathVariable Long id, @RequestBody(required = false) ProfitReportRejectRequest req) {
        String remark = req == null ? null : req.getRemark();
        profitReportService.rejectByDirectParent(id, SecurityUtils.requireUserId(), remark);
        return ApiResult.ok();
    }

    @GetMapping("/{id}")
    public ApiResult<ProfitReport> getOne(@PathVariable Long id) {
        return ApiResult.ok(profitReportService.getReportForViewer(id, SecurityUtils.requireUserId()));
    }

    @GetMapping("/{id}/flow")
    public ApiResult<ProfitReportFlowDetailVO> flow(@PathVariable Long id) {
        return ApiResult.ok(profitReportService.flowDetail(SecurityUtils.requireUserId(), id));
    }

    @PostMapping("/{id}/resubmit")
    public ApiResult<Void> resubmit(@PathVariable Long id, @Valid @RequestBody ProfitReportResubmitRequest req) {
        profitReportService.resubmit(SecurityUtils.requireUserId(), id, req);
        return ApiResult.ok();
    }
}

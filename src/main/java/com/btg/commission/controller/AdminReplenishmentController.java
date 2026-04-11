package com.btg.commission.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.v1.ProfitReportRejectRequest;
import com.btg.commission.dto.v1.ReplenishmentApproveDTO;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.RepayService;
import com.btg.commission.service.ReplenishmentService;
import com.btg.commission.vo.ReplenishmentApplyVO;
import com.btg.commission.vo.RepayApplyVO;
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

/**
 * 资方端（根用户 ROLE_ADMIN）：补仓与归仓审核。
 */
@Tag(name = "管理-补仓与归仓")
@RestController
@RequestMapping("/api/admin/replenishments")
@RequiredArgsConstructor
public class AdminReplenishmentController {

    private final ReplenishmentService replenishmentService;
    private final RepayService repayService;

    @GetMapping("/pending")
    public ApiResult<Page<ReplenishmentApplyVO>> pending(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(replenishmentService.pagePendingForAdmin(page, size));
    }

    @PostMapping("/{id}/approve")
    public ApiResult<Void> approveReplenishment(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReplenishmentApproveDTO dto) {
        replenishmentService.approveForAdmin(id, SecurityUtils.requireUserId(), dto);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/reject")
    public ApiResult<Void> rejectReplenishment(
            @PathVariable("id") Long id,
            @RequestBody(required = false) ProfitReportRejectRequest req) {
        String remark = req == null ? null : req.getRemark();
        replenishmentService.rejectForAdmin(id, SecurityUtils.requireUserId(), remark);
        return ApiResult.ok();
    }

    @GetMapping("/repays/pending")
    public ApiResult<Page<RepayApplyVO>> repaysPending(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(repayService.pagePendingForAdmin(page, size));
    }

    @PostMapping("/repays/{id}/approve")
    public ApiResult<Void> approveRepay(
            @PathVariable("id") Long id,
            @RequestBody(required = false) ProfitReportRejectRequest req) {
        String remark = req == null ? null : req.getRemark();
        repayService.approveForAdmin(id, SecurityUtils.requireUserId(), remark);
        return ApiResult.ok();
    }

    @PostMapping("/repays/{id}/reject")
    public ApiResult<Void> rejectRepay(
            @PathVariable("id") Long id,
            @RequestBody(required = false) ProfitReportRejectRequest req) {
        String remark = req == null ? null : req.getRemark();
        repayService.rejectForAdmin(id, SecurityUtils.requireUserId(), remark);
        return ApiResult.ok();
    }
}

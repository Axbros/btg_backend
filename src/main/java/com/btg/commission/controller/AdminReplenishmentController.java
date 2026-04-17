package com.btg.commission.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.v1.AdminReplenishmentApproveRequest;
import com.btg.commission.dto.v1.ProfitReportRejectRequest;
import com.btg.commission.dto.v1.ReplenishmentAssignCapitalRequest;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.RepayService;
import com.btg.commission.service.ReplenishmentService;
import com.btg.commission.vo.ReplenishmentApplyVO;
import com.btg.commission.vo.ReplenishmentPendingBriefVO;
import com.btg.commission.vo.RepayApplyVO;
import io.swagger.v3.oas.annotations.Operation;
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
 * 管理端（根用户）：补仓审核、转派资方、全量查询；归仓审核由补仓执行人在玩家端处理。
 */
@Tag(name = "管理-补仓与归仓")
@RestController
@RequestMapping("${btg.api.base-path}/admin/replenishments")
@RequiredArgsConstructor
public class AdminReplenishmentController {

    private final ReplenishmentService replenishmentService;
    private final RepayService repayService;

    @Operation(summary = "待管理员审核补仓分页", description = "状态 PENDING_ADMIN_REVIEW；每条 id、nickname、mobile、replenishAmount；完整字段见 GET …/admin/replenishments/{id}")
    @GetMapping("/pending")
    public ApiResult<Page<ReplenishmentPendingBriefVO>> pending(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(replenishmentService.pagePendingForAdmin(page, size));
    }

    @Operation(summary = "全部补仓单分页", description = "管理员查看所有申请及资方、到账确认等字段")
    @GetMapping("/all")
    public ApiResult<Page<ReplenishmentApplyVO>> allReplenishments(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(replenishmentService.pageAllForAdmin(page, size));
    }

    @Operation(summary = "补仓申请详情（管理员）", description = "完整 ReplenishmentApplyVO（含 wallet、凭证、状态等）")
    @GetMapping("/{id:\\d+}")
    public ApiResult<ReplenishmentApplyVO> replenishmentDetail(@PathVariable("id") Long id) {
        return ApiResult.ok(replenishmentService.getReplenishmentDetailForAdmin(id));
    }

    @Operation(summary = "管理员审核通过", description = "PENDING_ADMIN_REVIEW → ASSIGNED_TO_CAPITAL（待转派资方执行人）")
    @PostMapping("/{id}/approve")
    public ApiResult<Void> approveReplenishment(
            @PathVariable("id") Long id,
            @RequestBody(required = false) @Valid AdminReplenishmentApproveRequest req) {
        String remark = req == null ? null : req.getRemark();
        replenishmentService.approveByAdmin(id, SecurityUtils.requireUserId(), remark);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/reject")
    public ApiResult<Void> rejectReplenishment(
            @PathVariable("id") Long id,
            @RequestBody(required = false) ProfitReportRejectRequest req) {
        String remark = req == null ? null : req.getRemark();
        replenishmentService.rejectByAdmin(id, SecurityUtils.requireUserId(), remark);
        return ApiResult.ok();
    }

    @Operation(summary = "转派资方执行人", description = "ASSIGNED_TO_CAPITAL → PENDING_CAPITAL_SUBMIT")
    @PostMapping("/{id}/assign")
    public ApiResult<Void> assignCapital(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReplenishmentAssignCapitalRequest req) {
        replenishmentService.assignCapital(id, SecurityUtils.requireUserId(), req);
        return ApiResult.ok();
    }

    @GetMapping("/repays/{id}")
    @Operation(summary = "归仓申请详情（管理员）", description = "含申请人 nickname、mobile；replenishmentApply 为 replenishApplyId 对应补仓单完整信息；归仓审核请在玩家端由补仓执行人处理")
    public ApiResult<RepayApplyVO> repayDetail(@PathVariable("id") Long id) {
        return ApiResult.ok(repayService.getAdminRepayDetail(id));
    }
}

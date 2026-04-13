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
import com.btg.commission.vo.RepayPendingBriefVO;
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
 * 资方端（根用户 ROLE_ADMIN）：补仓与归仓审核。
 */
@Tag(name = "管理-补仓与归仓")
@RestController
@RequestMapping("${btg.api.base-path}/admin/replenishments")
@RequiredArgsConstructor
public class AdminReplenishmentController {

    private final ReplenishmentService replenishmentService;
    private final RepayService repayService;

    @Operation(summary = "待处理补仓分页", description = "含待受理(1)、待上传凭证(7)、待终审(8)；含申请人 nickname、mobile、walletName、walletAddress")
    @GetMapping("/pending")
    public ApiResult<Page<ReplenishmentApplyVO>> pending(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(replenishmentService.pagePendingForAdmin(page, size));
    }

    @Operation(summary = "受理补仓申请", description = "待审核(1) → 待资方上传凭证(7)")
    @PostMapping("/{id}/accept")
    public ApiResult<Void> acceptReplenishment(@PathVariable("id") Long id) {
        replenishmentService.acceptForAdmin(id, SecurityUtils.requireUserId());
        return ApiResult.ok();
    }

    @Operation(summary = "资方上传转账凭证与备注", description = "状态 7 → 8；body 同 ReplenishmentApproveDTO（transferScreenshotUrl 必填，transferRemark 选填）")
    @PostMapping("/{id}/capital-voucher")
    public ApiResult<Void> submitCapitalVoucher(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReplenishmentApproveDTO dto) {
        replenishmentService.submitCapitalVoucherForAdmin(SecurityUtils.requireUserId(), id, dto);
        return ApiResult.ok();
    }

    @Operation(summary = "资方终审确认", description = "状态 8 → 2；无请求体，仅确认；凭证须已在上一步写入")
    @PostMapping("/{id}/approve")
    public ApiResult<Void> approveReplenishment(@PathVariable("id") Long id) {
        replenishmentService.approveForAdmin(id, SecurityUtils.requireUserId());
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
    @Operation(summary = "待审核归仓分页", description = "每条 id、repayNo、status；完整字段见 GET …/repays/{id}")
    public ApiResult<Page<RepayPendingBriefVO>> repaysPending(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(repayService.pagePendingForAdmin(page, size));
    }

    @GetMapping("/repays/{id}")
    @Operation(summary = "归仓申请详情（资方）", description = "含申请人 nickname、mobile；replenishmentApply 为 replenishApplyId 对应补仓单完整信息")
    public ApiResult<RepayApplyVO> repayDetail(@PathVariable("id") Long id) {
        return ApiResult.ok(repayService.getAdminRepayDetail(id));
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

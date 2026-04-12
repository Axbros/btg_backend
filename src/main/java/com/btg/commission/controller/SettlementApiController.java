package com.btg.commission.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.v1.SettlementRejectRequest;
import com.btg.commission.dto.v1.SettlementSubmitRequest;
import com.btg.commission.entity.SettlementOrder;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.SettlementOrderService;
import com.btg.commission.vo.SettlementOrderDetailVo;
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

@Tag(name = "逐级结算")
@RestController
@RequestMapping("${btg.api.base-path}/settlements")
@RequiredArgsConstructor
public class SettlementApiController {

    private final SettlementOrderService settlementOrderService;

    @GetMapping("/mine-payables")
    public ApiResult<Page<SettlementOrder>> minePayables(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(settlementOrderService.pageMinePayables(SecurityUtils.requireUserId(), page, size));
    }

    @GetMapping("/pending-review")
    public ApiResult<Page<SettlementOrder>> pendingReview(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(settlementOrderService.pagePendingReview(SecurityUtils.requireUserId(), page, size));
    }

    @PostMapping("/{id}/submit")
    public ApiResult<Void> submit(@PathVariable Long id, @Valid @RequestBody SettlementSubmitRequest req) {
        settlementOrderService.submitTransferProof(id, SecurityUtils.requireUserId(), req.getTransferScreenshotUrl());
        return ApiResult.ok();
    }

    @PostMapping("/{id}/approve")
    public ApiResult<Void> approve(@PathVariable Long id, @RequestBody(required = false) SettlementRejectRequest req) {
        String remark = req == null ? null : req.getRemark();
        settlementOrderService.approve(id, SecurityUtils.requireUserId(), remark);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/reject")
    public ApiResult<Void> reject(@PathVariable Long id, @RequestBody(required = false) SettlementRejectRequest req) {
        settlementOrderService.reject(id, SecurityUtils.requireUserId(), req == null ? null : req.getRemark());
        return ApiResult.ok();
    }

    @Operation(summary = "结算单详情（按行主键）", description = "路径为结算单 id；当前用户须为付款人或收款人。含 reportNo、收付款人信息、利润/划转截图 URL。")
    @GetMapping("/row/{settlementId}")
    public ApiResult<SettlementOrderDetailVo> getOneBySettlementRow(@PathVariable Long settlementId) {
        return ApiResult.ok(settlementOrderService.getDetailBySettlementIdForParty(settlementId, SecurityUtils.requireUserId()));
    }

    @Operation(summary = "结算单详情（付款人视角）", description = "路径 id 为 root_report_id，且仅返回 from_user_id 为当前用户的结算单")
    @GetMapping("/{rootReportId}")
    public ApiResult<SettlementOrderDetailVo> getOneByRootReportForPayer(@PathVariable Long rootReportId) {
        return ApiResult.ok(settlementOrderService.getDetailByRootReportForPayer(rootReportId, SecurityUtils.requireUserId()));
    }
}

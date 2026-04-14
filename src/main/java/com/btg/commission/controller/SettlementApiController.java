package com.btg.commission.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.v1.SettlementRejectRequest;
import com.btg.commission.dto.v1.SettlementSubmitRequest;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.SettlementOrderService;
import com.btg.commission.vo.SettlementOrderDetailVo;
import com.btg.commission.vo.flow.SettlementScopedProfitFlowVO;
import com.btg.commission.vo.SettlementOrderListItemVo;
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
    public ApiResult<Page<SettlementOrderListItemVo>> minePayables(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(settlementOrderService.pageMinePayables(SecurityUtils.requireUserId(), page, size));
    }

    @GetMapping("/pending-review")
    public ApiResult<Page<SettlementOrderListItemVo>> pendingReview(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(settlementOrderService.pagePendingReview(SecurityUtils.requireUserId(), page, size));
    }

    @Operation(summary = "已通过结算单（本人为收款上级）")
    @GetMapping("/approved")
    public ApiResult<Page<SettlementOrderListItemVo>> approved(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(settlementOrderService.pageApprovedAsReviewer(SecurityUtils.requireUserId(), page, size));
    }

    @Operation(summary = "已拒绝结算单（本人为收款上级）")
    @GetMapping("/rejected")
    public ApiResult<Page<SettlementOrderListItemVo>> rejected(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(settlementOrderService.pageRejectedAsReviewer(SecurityUtils.requireUserId(), page, size));
    }

    @Operation(summary = "全部（待审+已通过+已拒绝，本人为收款上级）")
    @GetMapping("/review-all")
    public ApiResult<Page<SettlementOrderListItemVo>> reviewAll(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(settlementOrderService.pageAllReviewStatesAsReviewer(SecurityUtils.requireUserId(), page, size));
    }

    @PostMapping("/{id:\\d+}/submit")
    public ApiResult<Void> submit(@PathVariable Long id, @Valid @RequestBody SettlementSubmitRequest req) {
        settlementOrderService.submitTransferProof(id, SecurityUtils.requireUserId(), req.getTransferScreenshotUrl());
        return ApiResult.ok();
    }

    @PostMapping("/{id:\\d+}/approve")
    public ApiResult<Void> approve(@PathVariable Long id, @RequestBody(required = false) SettlementRejectRequest req) {
        String remark = req == null ? null : req.getRemark();
        settlementOrderService.approve(id, SecurityUtils.requireUserId(), remark);
        return ApiResult.ok();
    }

    @PostMapping("/{id:\\d+}/reject")
    public ApiResult<Void> reject(@PathVariable Long id, @RequestBody(required = false) SettlementRejectRequest req) {
        settlementOrderService.reject(id, SecurityUtils.requireUserId(), req == null ? null : req.getRemark());
        return ApiResult.ok();
    }

    @Operation(summary = "结算单详情（按行主键）", description = "路径为结算单 id；当前用户须为付款人或收款人。含 reportNo、收付款人信息、利润/划转截图 URL。")
    @GetMapping("/row/{settlementId:\\d+}")
    public ApiResult<SettlementOrderDetailVo> getOneBySettlementRow(@PathVariable Long settlementId) {
        return ApiResult.ok(settlementOrderService.getDetailBySettlementIdForParty(settlementId, SecurityUtils.requireUserId()));
    }

    @Operation(summary = "结算单详情（付款人视角）", description = "路径 id 为 root_report_id（仅数字），且仅返回 from_user_id 为当前用户的结算单；避免与 /rejected、/approved 等字面路径冲突")
    @GetMapping("/{rootReportId:\\d+}")
    public ApiResult<SettlementOrderDetailVo> getOneByRootReportForPayer(@PathVariable Long rootReportId) {
        return ApiResult.ok(settlementOrderService.getDetailByRootReportForPayer(rootReportId, SecurityUtils.requireUserId()));
    }

    @Operation(summary = "利润链层级进度（按当前用户裁剪）",
            description = "路径 id 为 root_report_id。返回 layers：直属审利润（若可见）+ 可见范围内逐级结算每层状态（待提交/待审/通过/拒绝）；"
                    + "不含结算单审核流水与明细。申报人/根见全链；上级仅见申报人→本人路径；其余见本人子树与链交集。")
    @GetMapping("/{rootReportId:\\d+}/profit-flow")
    public ApiResult<SettlementScopedProfitFlowVO> profitFlow(@PathVariable Long rootReportId) {
        return ApiResult.ok(settlementOrderService.getScopedProfitFlowByRootReportId(rootReportId, SecurityUtils.requireUserId()));
    }
}

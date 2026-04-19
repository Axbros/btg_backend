package com.btg.commission.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.v1.SettlementRejectRequest;
import com.btg.commission.dto.v1.SettlementSubmitRequest;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.ProfitFlowDetailQuery;
import com.btg.commission.service.SettlementOrderService;
import com.btg.commission.vo.SettlementOrderDetailVo;
import com.btg.commission.vo.flow.ProfitFlowDetailVO;
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
    private final ProfitFlowDetailQuery profitFlowDetailQuery;

    @Operation(
            summary = "本人为付款人的结算单分页",
            description = "不传 status：本人为付款人的全部结算单。status=1～5：按状态精确筛选：1 INIT；2 待提交凭证；3 待上级审核；4 通过；5 拒绝。")
    @GetMapping("/mine-payables")
    public ApiResult<Page<SettlementOrderListItemVo>> minePayables(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status) {
        return ApiResult.ok(settlementOrderService.pageMinePayables(SecurityUtils.requireUserId(), page, size, status));
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

    @Operation(
            summary = "拒绝本级结算转账凭证",
            description = "仅收款上级（to_user）可拒。拒单后：本笔结算回到待提交凭证，由本笔付款人（from_user）重新走 POST …/settlements/{id}/submit；"
                    + "关联利润单不会进入 RETURNED_TO_APPLICANT(5)，current_handler 指向该付款人，而非根申报人。")
    @PostMapping("/{id:\\d+}/reject")
    public ApiResult<Void> reject(@PathVariable Long id, @RequestBody(required = false) SettlementRejectRequest req) {
        settlementOrderService.reject(id, SecurityUtils.requireUserId(), req == null ? null : req.getRemark());
        return ApiResult.ok();
    }

    @Operation(summary = "结算单详情（按行主键）", description = "路径为结算单 id；当前用户须为付款人或收款人。含 reportNo、申报人/上报金额、"
            + "上级(to)对下级(from)的分润配置比例 parentToChildProfitRatio、收付款人信息、利润/划转截图 URL。")
    @GetMapping("/row/{settlementId:\\d+}")
    public ApiResult<SettlementOrderDetailVo> getOneBySettlementRow(@PathVariable Long settlementId) {
        return ApiResult.ok(settlementOrderService.getDetailBySettlementIdForParty(settlementId, SecurityUtils.requireUserId()));
    }

    @Operation(summary = "结算单详情（付款人视角）", description = "路径 id 为 root_report_id（仅数字），且仅返回 from_user_id 为当前用户的结算单；避免与 /rejected、/approved 等字面路径冲突")
    @GetMapping("/{rootReportId:\\d+}")
    public ApiResult<SettlementOrderDetailVo> getOneByRootReportForPayer(@PathVariable Long rootReportId) {
        return ApiResult.ok(settlementOrderService.getDetailByRootReportForPayer(rootReportId, SecurityUtils.requireUserId()));
    }

    @Operation(
            summary = "利润分润链路详情",
            description = "路径 id 为 root_report_id。按总利润切片模型返回分润层级（upper/lower 比例、incomeAmount、payAmountToParent）、"
                    + "关联结算状态、当前处理人与备注。发起人的邀请链祖先上级与根用户见全量金额；申报人见全链状态但隐藏本人之上各层金额明细；"
                    + "其他链上参与用户隐藏其链上位置之上的金额明细。")
    @GetMapping("/{rootReportId:\\d+}/profit-flow")
    public ApiResult<ProfitFlowDetailVO> profitFlow(@PathVariable Long rootReportId) {
        return ApiResult.ok(profitFlowDetailQuery.getProfitFlowDetailByRootReportId(rootReportId, SecurityUtils.requireUserId()));
    }
}

package com.btg.commission.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.v1.ReplenishmentApplyDTO;
import com.btg.commission.dto.v1.ReplenishmentArrivalActionRequest;
import com.btg.commission.dto.v1.ReplenishmentCapitalSubmitRequest;
import com.btg.commission.dto.v1.ProfitReportRejectRequest;
import com.btg.commission.dto.v1.ReplenishmentResubmitRequest;
import com.btg.commission.dto.v1.RepayApplyDTO;
import com.btg.commission.dto.v1.RepayResubmitRequest;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.RepayService;
import com.btg.commission.service.ReplenishmentService;
import com.btg.commission.vo.ReplenishmentApplyBriefVO;
import com.btg.commission.vo.ReplenishmentApplyDetailVO;
import com.btg.commission.vo.ReplenishmentApplyVO;
import com.btg.commission.vo.ReplenishmentTeamItemVO;
import com.btg.commission.vo.RepayApplyVO;
import com.btg.commission.vo.RepayMineBriefVO;
import com.btg.commission.vo.RepayPendingReviewListItemVO;
import com.btg.commission.vo.RepayableReplenishmentVO;
import com.btg.commission.vo.flow.RepayApplyFlowDetailVO;
import com.btg.commission.vo.flow.ReplenishmentApplyFlowDetailVO;
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

import java.util.List;

/**
 * 玩家端：补仓 / 归仓申请。路径与现有 API 一致使用 {@code /api/v1} 前缀。
 */
@Tag(name = "补仓与归仓")
@RestController
@RequestMapping("${btg.api.base-path}/replenishments")
@RequiredArgsConstructor
public class ReplenishmentApiController {

    private final ReplenishmentService replenishmentService;
    private final RepayService repayService;

    @PostMapping
    public ApiResult<Long> submitReplenishment(@Valid @RequestBody ReplenishmentApplyDTO dto) {
        return ApiResult.ok(replenishmentService.submit(SecurityUtils.requireUserId(), dto));
    }

    @Operation(summary = "我的补仓申请分页", description = "每条含 status（后台 1～8）、userVisibleStatus（申请人简化 1～5）、applyNo、replenishAmount、submitTime。可选 query：userVisibleStatus=1～5 时在库表按对应后台状态子集筛选分页（与展示口径一致）；详情见 GET …/replenishments/{id}")
    @GetMapping("/mine")
    public ApiResult<Page<ReplenishmentApplyBriefVO>> mine(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer userVisibleStatus) {
        return ApiResult.ok(replenishmentService.pageMine(SecurityUtils.requireUserId(), page, size, userVisibleStatus));
    }

    @Operation(summary = "我被转派的补仓单", description = "资方执行人待提交或退回修改")
    @GetMapping("/assigned-to-me")
    public ApiResult<Page<ReplenishmentApplyBriefVO>> assignedToMe(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(replenishmentService.pageAssignedToMe(SecurityUtils.requireUserId(), page, size));
    }

    /** 当前未结清补仓（SUCCESS 且剩余应还大于 0），无则 data 为 null */
    @GetMapping("/current")
    public ApiResult<ReplenishmentApplyVO> current() {
        return ApiResult.ok(replenishmentService.current(SecurityUtils.requireUserId()));
    }

    @Operation(summary = "下级补仓记录分页", description = "不含本人；每条 id、status、nickname、mobile、replenishAmount")
    @GetMapping("/team")
    public ApiResult<Page<ReplenishmentTeamItemVO>> teamReplenishments(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(replenishmentService.pageTeamDescendantApplies(SecurityUtils.requireUserId(), page, size));
    }

    @Operation(summary = "可归仓的补仓单列表", description = "补仓成功且剩余应还大于 0；提交归仓须传 replenishApplyId")
    @GetMapping("/repayable")
    public ApiResult<List<RepayableReplenishmentVO>> repayableReplenishments() {
        return ApiResult.ok(repayService.listRepayableReplenishments(SecurityUtils.requireUserId()));
    }

    @PostMapping("/repays")
    public ApiResult<Long> submitRepay(@Valid @RequestBody RepayApplyDTO dto) {
        return ApiResult.ok(repayService.submit(SecurityUtils.requireUserId(), dto));
    }

    @Operation(summary = "我的归仓申请分页", description = "仅 id、repayNo、status、repayAmount；详情见 GET …/repays/{id}")
    @GetMapping("/repays/mine")
    public ApiResult<Page<RepayMineBriefVO>> repaysMine(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(repayService.pageMine(SecurityUtils.requireUserId(), page, size));
    }

    @Operation(summary = "待我审核的归仓申请（补仓执行方）", description = "capital_user_id = 当前用户且状态待资方审核。每条仅 id、repayNo、pendingRepayAmount（关联补仓单 pending_repay_amount）；详情 GET …/repays/{id}")
    @GetMapping("/repays/pending-review")
    public ApiResult<Page<RepayPendingReviewListItemVO>> repaysPendingReview(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(repayService.pagePendingReviewForCapital(SecurityUtils.requireUserId(), page, size));
    }

    @Operation(summary = "下级归仓记录分页", description = "不含本人；每条与 GET …/replenishments/team 相同：id、status、nickname、mobile、replenishAmount（此处为 repay_amount）")
    @GetMapping("/repays/team")
    public ApiResult<Page<ReplenishmentTeamItemVO>> teamRepays(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(repayService.pageTeamDescendantRepays(SecurityUtils.requireUserId(), page, size));
    }

    @Operation(summary = "归仓申请详情（本人或团队长）", description = "含 replenishmentApply 等完整字段")
    @GetMapping("/repays/{id:\\d+}")
    public ApiResult<RepayApplyVO> repayDetail(@PathVariable("id") Long id) {
        return ApiResult.ok(repayService.getRepayDetailForUser(SecurityUtils.requireUserId(), id));
    }

    @Operation(summary = "补仓申请详情（本人或团队长）", description = "status 为补仓状态码；replenishment 为完整补仓信息；approvedRepays 为本补仓单已审核通过的归仓记录")
    @GetMapping("/{id:\\d+}")
    public ApiResult<ReplenishmentApplyDetailVO> replenishmentDetail(@PathVariable("id") Long id) {
        return ApiResult.ok(replenishmentService.getReplenishmentDetailForUser(SecurityUtils.requireUserId(), id));
    }

    @GetMapping("/{id:\\d+}/flow")
    public ApiResult<ReplenishmentApplyFlowDetailVO> replenishmentFlow(@PathVariable("id") Long id) {
        return ApiResult.ok(replenishmentService.flowDetail(SecurityUtils.requireUserId(), id));
    }

    @PostMapping("/{id:\\d+}/resubmit")
    public ApiResult<Void> replenishmentResubmit(@PathVariable("id") Long id, @Valid @RequestBody ReplenishmentResubmitRequest req) {
        replenishmentService.resubmit(SecurityUtils.requireUserId(), id, req);
        return ApiResult.ok();
    }

    @Operation(summary = "资方同意并提交补仓转账凭证", description = "被转派的资方执行人：上传转账凭证；补仓单上的资方收款 UID 取本人 user_profile.exchange_uid（勿在请求体传 UID）。PENDING_CAPITAL_SUBMIT / RETURNED_TO_CAPITAL → 待申请人确认到账。根用户不可调用。")
    @PostMapping("/{id:\\d+}/capital-submit")
    public ApiResult<Void> capitalSubmit(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReplenishmentCapitalSubmitRequest req) {
        replenishmentService.capitalSubmit(SecurityUtils.requireUserId(), id, req);
        return ApiResult.ok();
    }

    @Operation(summary = "资方拒绝执行补仓", description = "被转派的资方执行人拒绝执行并退回待根用户重新转派；需填写原因。根用户不可调用。")
    @PostMapping("/{id:\\d+}/capital-reject-assignment")
    public ApiResult<Void> rejectCapitalAssignment(
            @PathVariable("id") Long id,
            @RequestBody(required = false) @Valid ProfitReportRejectRequest req) {
        String remark = req == null ? null : req.getRemark();
        replenishmentService.rejectCapitalAssignment(SecurityUtils.requireUserId(), id, remark);
        return ApiResult.ok();
    }

    @Operation(summary = "申请人确认到账", description = "PENDING_APPLICANT_CONFIRM → SUCCESS")
    @PostMapping("/{id:\\d+}/confirm-arrival")
    public ApiResult<Void> confirmArrival(
            @PathVariable("id") Long id,
            @RequestBody(required = false) @Valid ReplenishmentArrivalActionRequest req) {
        String remark = req == null ? null : req.getRemark();
        replenishmentService.confirmArrival(SecurityUtils.requireUserId(), id, remark);
        return ApiResult.ok();
    }

    @Operation(summary = "申请人拒绝到账", description = "退回资方执行人修改凭证")
    @PostMapping("/{id:\\d+}/reject-arrival")
    public ApiResult<Void> rejectArrival(
            @PathVariable("id") Long id,
            @RequestBody(required = false) @Valid ReplenishmentArrivalActionRequest req) {
        String remark = req == null ? null : req.getRemark();
        replenishmentService.rejectArrival(SecurityUtils.requireUserId(), id, remark);
        return ApiResult.ok();
    }

    @GetMapping("/repays/{id:\\d+}/flow")
    public ApiResult<RepayApplyFlowDetailVO> repayFlow(@PathVariable("id") Long id) {
        return ApiResult.ok(repayService.flowDetail(SecurityUtils.requireUserId(), id));
    }

    @PostMapping("/repays/{id:\\d+}/resubmit")
    public ApiResult<Void> repayResubmit(@PathVariable("id") Long id, @Valid @RequestBody RepayResubmitRequest req) {
        repayService.resubmit(SecurityUtils.requireUserId(), id, req);
        return ApiResult.ok();
    }

    @Operation(summary = "归仓审核通过（补仓执行方）")
    @PostMapping("/repays/{id:\\d+}/approve")
    public ApiResult<Void> repayApprove(
            @PathVariable("id") Long id,
            @RequestBody(required = false) @Valid ProfitReportRejectRequest req) {
        String remark = req == null ? null : req.getRemark();
        repayService.approveRepay(SecurityUtils.requireUserId(), id, remark);
        return ApiResult.ok();
    }

    @Operation(summary = "归仓退回申请人修改（补仓执行方）")
    @PostMapping("/repays/{id:\\d+}/reject")
    public ApiResult<Void> repayReject(
            @PathVariable("id") Long id,
            @RequestBody(required = false) @Valid ProfitReportRejectRequest req) {
        String remark = req == null ? null : req.getRemark();
        repayService.rejectRepay(SecurityUtils.requireUserId(), id, remark);
        return ApiResult.ok();
    }
}

package com.btg.commission.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.v1.ReplenishmentApplyDTO;
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
import com.btg.commission.vo.RepayPendingBriefVO;
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

    @Operation(summary = "我的补仓申请分页", description = "每条 id、applyNo、status；完整信息及成功归仓记录见 GET …/replenishments/{id}")
    @GetMapping("/mine")
    public ApiResult<Page<ReplenishmentApplyBriefVO>> mine(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(replenishmentService.pageMine(SecurityUtils.requireUserId(), page, size));
    }

    /** 当前未结清补仓（状态为审核通过或部分归还），无则 data 为 null */
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

    @Operation(summary = "可归仓的补仓单列表", description = "审核通过或部分归还、剩余应还大于 0；提交归仓须传 replenishApplyId")
    @GetMapping("/repayable")
    public ApiResult<List<RepayableReplenishmentVO>> repayableReplenishments() {
        return ApiResult.ok(repayService.listRepayableReplenishments(SecurityUtils.requireUserId()));
    }

    @PostMapping("/repays")
    public ApiResult<Long> submitRepay(@Valid @RequestBody RepayApplyDTO dto) {
        return ApiResult.ok(repayService.submit(SecurityUtils.requireUserId(), dto));
    }

    @Operation(summary = "我的归仓申请分页", description = "含关联补仓单号与金额摘要；完整信息见 GET …/repays/{id}")
    @GetMapping("/repays/mine")
    public ApiResult<Page<RepayPendingBriefVO>> repaysMine(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(repayService.pageMine(SecurityUtils.requireUserId(), page, size));
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

    @Operation(summary = "补仓申请详情（本人或团队长）", description = "replenishment 为完整补仓信息；approvedRepays 为审核通过的归仓成功记录")
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

    @GetMapping("/repays/{id:\\d+}/flow")
    public ApiResult<RepayApplyFlowDetailVO> repayFlow(@PathVariable("id") Long id) {
        return ApiResult.ok(repayService.flowDetail(SecurityUtils.requireUserId(), id));
    }

    @PostMapping("/repays/{id:\\d+}/resubmit")
    public ApiResult<Void> repayResubmit(@PathVariable("id") Long id, @Valid @RequestBody RepayResubmitRequest req) {
        repayService.resubmit(SecurityUtils.requireUserId(), id, req);
        return ApiResult.ok();
    }
}

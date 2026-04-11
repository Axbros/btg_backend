package com.btg.commission.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ApiResult;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.CommissionQueryService;
import com.btg.commission.vo.CommissionMineListItemVo;
import com.btg.commission.vo.CommissionRecordVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "我的佣金")
@RestController
@RequestMapping("/api/commissions")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionQueryService commissionQueryService;

    @Operation(summary = "我的佣金分页", description = "records 仅含 id、profitRecordNo、profitAmount、commissionAmount、commissionRate、status；详情用 GET /api/commissions/mine/{id}")
    @GetMapping("/mine")
    public ApiResult<Page<CommissionMineListItemVo>> mine(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(commissionQueryService.pageReceived(SecurityUtils.requireUserId(), page, size));
    }

    @Operation(summary = "我的佣金详情", description = "仅可查看收款方为当前用户的流水；完整字段含 profitRecordId、fromUserId、strategyId、confirmedTime、来源昵称/手机、策略名等。")
    @GetMapping("/mine/{id}")
    public ApiResult<CommissionRecordVo> mineDetail(@PathVariable Long id) {
        CommissionRecordVo vo = commissionQueryService.getReceivedDetail(SecurityUtils.requireUserId(), id);
        if (vo == null) {
            throw new BizException(ResultCode.NOT_FOUND, "佣金流水不存在");
        }
        return ApiResult.ok(vo);
    }
}

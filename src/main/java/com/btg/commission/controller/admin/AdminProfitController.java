package com.btg.commission.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.profit.ProfitAuditRequest;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.ProfitAuditService;
import com.btg.commission.service.ProfitRecordService;
import com.btg.commission.vo.ProfitRecordVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/profits")
@RequiredArgsConstructor
public class AdminProfitController {

    private final ProfitAuditService profitAuditService;
    private final ProfitRecordService profitRecordService;

    @GetMapping("/pending")
    public ApiResult<Page<ProfitRecordVo>> pending(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(profitRecordService.pagePending(page, size));
    }

    @PostMapping("/approve")
    public ApiResult<Void> approve(@Valid @RequestBody ProfitAuditRequest req) {
        profitAuditService.approve(req.getProfitRecordId(), SecurityUtils.requireUserId(), req.getRemark());
        return ApiResult.ok();
    }

    @PostMapping("/reject")
    public ApiResult<Void> reject(@Valid @RequestBody ProfitAuditRequest req) {
        profitAuditService.reject(req.getProfitRecordId(), SecurityUtils.requireUserId(), req.getRemark());
        return ApiResult.ok();
    }
}

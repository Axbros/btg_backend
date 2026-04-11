package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.kyc.KycAuditRequest;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.KycAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "KYC 审核")
@RestController
@RequestMapping("/api/kyc/audit")
@RequiredArgsConstructor
public class KycAuditController {

    private final KycAuditService kycAuditService;

    @Operation(summary = "KYC 审核通过", description = "被审核用户资料须为待审核；操作人须为其直属上级或任意上级。")
    @PostMapping("/approve")
    public ApiResult<Void> approve(@Valid @RequestBody KycAuditRequest req) {
        kycAuditService.approve(SecurityUtils.requireUserId(), req);
        return ApiResult.ok();
    }

    @Operation(summary = "KYC 审核拒绝", description = "被审核用户须为待审核；操作人须为其直属上级或任意上级。")
    @PostMapping("/reject")
    public ApiResult<Void> reject(@Valid @RequestBody KycAuditRequest req) {
        kycAuditService.reject(SecurityUtils.requireUserId(), req);
        return ApiResult.ok();
    }
}

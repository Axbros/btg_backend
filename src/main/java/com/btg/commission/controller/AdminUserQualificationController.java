package com.btg.commission.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.admin.QualificationAuditRequest;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.UserQualificationService;
import com.btg.commission.vo.PendingQualificationUserVO;
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

@Tag(name = "管理-新成员资格审核")
@RestController
@RequestMapping("${btg.api.base-path}/admin/users")
@RequiredArgsConstructor
public class AdminUserQualificationController {

    private final UserQualificationService userQualificationService;

    @Operation(summary = "待系统管理员资格审核用户分页")
    @GetMapping("/pending-qualification")
    public ApiResult<Page<PendingQualificationUserVO>> pendingQualification(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(userQualificationService.pagePendingQualification(page, size));
    }

    @Operation(summary = "系统管理员审核通过（资格）")
    @PostMapping("/{id:\\d+}/approve-qualification")
    public ApiResult<Void> approveQualification(
            @PathVariable("id") Long userId,
            @Valid @RequestBody(required = false) QualificationAuditRequest req) {
        String remark = req == null ? null : req.getRemark();
        userQualificationService.approveQualification(userId, SecurityUtils.requireUserId(), remark);
        return ApiResult.ok();
    }

    @Operation(summary = "系统管理员审核拒绝（资格）")
    @PostMapping("/{id:\\d+}/reject-qualification")
    public ApiResult<Void> rejectQualification(
            @PathVariable("id") Long userId,
            @Valid @RequestBody(required = false) QualificationAuditRequest req) {
        String remark = req == null ? null : req.getRemark();
        userQualificationService.rejectQualification(userId, SecurityUtils.requireUserId(), remark);
        return ApiResult.ok();
    }
}

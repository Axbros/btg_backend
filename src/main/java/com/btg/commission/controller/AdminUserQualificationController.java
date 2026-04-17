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

/**
 * 新成员「资格审核」仅允许根用户操作，后端三层一致拦截（不可仅靠前端隐藏）：
 * <ol>
 *   <li>Spring Security：{@code ${btg.api.base-path}/admin/**} → {@code hasRole("ADMIN")}</li>
 *   <li>JWT 中 {@code ROLE_ADMIN} 来自 {@link com.btg.commission.security.LoginUserService} 对 {@code btg_user.is_root} 的判定（与 {@link com.btg.commission.security.LoginUser#isAdmin()} 一致）</li>
 *   <li>本 Controller {@link com.btg.commission.security.SecurityUtils#requireRootUser()} + {@link UserQualificationService} 内对操作人再次查库校验 {@code btg_user.is_root}</li>
 * </ol>
 */
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
        Long operatorId = SecurityUtils.requireRootUser().getUserId();
        return ApiResult.ok(userQualificationService.pagePendingQualification(operatorId, page, size));
    }

    @Operation(summary = "系统管理员审核通过（资格）")
    @PostMapping("/{id:\\d+}/approve-qualification")
    public ApiResult<Void> approveQualification(
            @PathVariable("id") Long userId,
            @Valid @RequestBody(required = false) QualificationAuditRequest req) {
        Long operatorId = SecurityUtils.requireRootUser().getUserId();
        String remark = req == null ? null : req.getRemark();
        userQualificationService.approveQualification(userId, operatorId, remark);
        return ApiResult.ok();
    }

    @Operation(summary = "系统管理员审核拒绝（资格）")
    @PostMapping("/{id:\\d+}/reject-qualification")
    public ApiResult<Void> rejectQualification(
            @PathVariable("id") Long userId,
            @Valid @RequestBody(required = false) QualificationAuditRequest req) {
        Long operatorId = SecurityUtils.requireRootUser().getUserId();
        String remark = req == null ? null : req.getRemark();
        userQualificationService.rejectQualification(userId, operatorId, remark);
        return ApiResult.ok();
    }
}

package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.admin.QualificationAuditRequest;
import com.btg.commission.entity.UserProfitConfig;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.UserProfitConfigService;
import com.btg.commission.vo.ProfitConfigModeAuditDetailVO;
import com.btg.commission.vo.UserProfitConfigListItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理-分润模式审核")
@RestController
@RequestMapping("${btg.api.base-path}/admin/profit-configs")
@RequiredArgsConstructor
public class AdminProfitConfigAuditController {

    private final UserProfitConfigService userProfitConfigService;

    @Operation(summary = "待审核分润模式切换列表")
    @GetMapping("/pending-mode-audits")
    public ApiResult<List<UserProfitConfigListItemVO>> pendingModeAudits() {
        SecurityUtils.requireRootUser();
        return ApiResult.ok(userProfitConfigService.listPendingModeAudits());
    }

    @Operation(summary = "待审核分润模式切换详情")
    @GetMapping("/{id:\\d+}/mode-change-detail")
    public ApiResult<ProfitConfigModeAuditDetailVO> pendingModeAuditDetail(@PathVariable("id") Long id) {
        SecurityUtils.requireRootUser();
        return ApiResult.ok(userProfitConfigService.getPendingModeAuditDetail(id));
    }

    @Operation(summary = "审核通过分润模式切换")
    @PostMapping("/{id:\\d+}/approve-mode-change")
    public ApiResult<UserProfitConfig> approveModeChange(
            @PathVariable("id") Long id,
            @Valid @RequestBody(required = false) QualificationAuditRequest req) {
        Long rootId = SecurityUtils.requireRootUser().getUserId();
        String remark = req == null ? null : req.getRemark();
        return ApiResult.ok(userProfitConfigService.approvePendingModeAudit(id, rootId, remark));
    }

    @Operation(summary = "审核拒绝分润模式切换")
    @PostMapping("/{id:\\d+}/reject-mode-change")
    public ApiResult<UserProfitConfig> rejectModeChange(
            @PathVariable("id") Long id,
            @Valid @RequestBody(required = false) QualificationAuditRequest req) {
        Long rootId = SecurityUtils.requireRootUser().getUserId();
        String remark = req == null ? null : req.getRemark();
        return ApiResult.ok(userProfitConfigService.rejectPendingModeAudit(id, rootId, remark));
    }
}

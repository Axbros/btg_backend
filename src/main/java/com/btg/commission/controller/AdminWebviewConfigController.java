package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.v1.WebviewConfigUpdateDTO;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.WebviewConfigService;
import com.btg.commission.vo.WebviewConfigDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理-WebView 网关")
@RestController
@RequestMapping("${btg.api.base-path}/admin/webview")
@RequiredArgsConstructor
public class AdminWebviewConfigController {

    private final WebviewConfigService webviewConfigService;

    @Operation(summary = "查看 WebView 网关配置")
    @GetMapping("/config")
    public ApiResult<WebviewConfigDetailVO> getConfig() {
        SecurityUtils.requireRootUser();
        return ApiResult.ok(webviewConfigService.getConfigForAdmin());
    }

    @Operation(summary = "更新 WebView 网关配置", description = "单条配置：无行则插入，有行则更新。webUrl 须 https://")
    @PutMapping("/config")
    public ApiResult<Void> updateConfig(@Valid @RequestBody WebviewConfigUpdateDTO dto) {
        Long operatorId = SecurityUtils.requireRootUser().getUserId();
        webviewConfigService.updateConfig(dto, operatorId);
        return ApiResult.ok();
    }
}

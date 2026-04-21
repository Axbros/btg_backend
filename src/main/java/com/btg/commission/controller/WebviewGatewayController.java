package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.service.WebviewConfigService;
import com.btg.commission.vo.WebviewBootstrapVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "WebView 网关")
@RestController
@RequestMapping("${btg.api.base-path}/webview")
@RequiredArgsConstructor
public class WebviewGatewayController {

    private final WebviewConfigService webviewConfigService;

    @Operation(summary = "安卓获取 WebView 启动配置", description = "匿名可访问；无库内配置时返回 enabled=false 等默认值。启动图由端上本地资源，本接口不返回图片 URL。")
    @GetMapping("/config")
    public ApiResult<WebviewBootstrapVO> bootstrapConfig() {
        return ApiResult.ok(webviewConfigService.getBootstrapConfig());
    }
}

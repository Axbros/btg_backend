package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(description = "安卓 WebView 启动配置（不含图片地址）")
public class WebviewBootstrapVO {

    Boolean enabled;

    String webUrl;

    String injectJs;

    String injectCss;

    @Schema(description = "是否显示启动屏（图在端上本地）")
    Boolean showSplash;

    Integer splashDurationMs;

    Long version;
}

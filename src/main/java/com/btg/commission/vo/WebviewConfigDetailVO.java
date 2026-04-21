package com.btg.commission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/** 管理端查看当前 WebView 配置（含库内元数据） */
@Value
@Builder
public class WebviewConfigDetailVO {

    Long id;

    Boolean enabled;

    String webUrl;

    String injectJs;

    String injectCss;

    Boolean showSplash;

    Integer splashDurationMs;

    Long version;

    String remark;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}

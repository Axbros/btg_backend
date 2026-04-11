package com.btg.commission.openapi;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 仅用于文档/Swagger：{@code POST /api/profits/referrer/approve} 与 {@code .../reject} 成功时的完整 JSON body（{@code data} 为 {@code null}）。
 */
@Data
@Schema(name = "ReferrerProfitAuditApiResponse", description = "POST .../approve、.../reject 成功响应体")
public class ReferrerProfitAuditApiResponse {

    @Schema(description = "业务码，成功为 200", example = "200")
    private int code;

    @Schema(description = "提示语", example = "success")
    private String message;

    @Schema(description = "无业务负载，固定为 null", nullable = true, example = "null")
    private Object data;
}

package com.btg.commission.openapi;

import com.btg.commission.vo.PageVo;
import com.btg.commission.vo.ReferrerProfitListItemVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>仅用于文档/Swagger：描述 {@code GET /api/profits/referrer/records} 成功时的<strong>完整 HTTP JSON body</strong>。
 * 运行时控制器返回 {@link com.btg.commission.common.api.ApiResult}{@code <}{@link PageVo}{@code <}{@link ReferrerProfitListItemVo}{@code >>}，与此结构一致。</p>
 */
@Data
@Schema(name = "ReferrerProfitListApiResponse", description = "GET /api/profits/referrer/records 成功响应体")
public class ReferrerProfitListApiResponse {

    @Schema(description = "业务码，成功为 200", example = "200")
    private int code;

    @Schema(description = "提示语", example = "success")
    private String message;

    @Schema(description = "分页数据：records + total + page + pageSize（列表项为精简字段）")
    private PageVo<ReferrerProfitListItemVo> data;
}

package com.btg.commission.openapi;

import com.btg.commission.vo.ReferrerProfitRecordDetailVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 仅用于文档/Swagger：{@code GET /api/profits/referrer/records/{id}} 成功时的完整 JSON body。
 */
@Data
@Schema(name = "ReferrerProfitDetailApiResponse", description = "GET /api/profits/referrer/records/{id} 成功响应体")
public class ReferrerProfitDetailApiResponse {

    @Schema(description = "业务码，成功为 200", example = "200")
    private int code;

    @Schema(description = "提示语", example = "success")
    private String message;

    @Schema(description = "单条收益申报（昵称与策略名，无 userId / referrerUserId / strategyId）")
    private ReferrerProfitRecordDetailVo data;
}

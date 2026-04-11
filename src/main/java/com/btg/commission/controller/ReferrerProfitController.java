package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.profit.ProfitAuditRequest;
import com.btg.commission.enums.ProfitRecordStatus;
import com.btg.commission.openapi.ReferrerProfitAuditApiResponse;
import com.btg.commission.openapi.ReferrerProfitDetailApiResponse;
import com.btg.commission.openapi.ReferrerProfitListApiResponse;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.ProfitAuditService;
import com.btg.commission.service.ProfitRecordService;
import com.btg.commission.vo.PageVo;
import com.btg.commission.vo.ReferrerProfitListItemVo;
import com.btg.commission.vo.ReferrerProfitRecordDetailVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

@Tag(name = "收益申报-直属上级审核")
@RestController
@RequestMapping("/api/profits/referrer")
@RequiredArgsConstructor
public class ReferrerProfitController {

    private final ProfitRecordService profitRecordService;
    private final ProfitAuditService profitAuditService;

    @Operation(summary = "下级收益申报列表", description = "仅包含「申报人直属推荐人为当前用户」的单据；可选按状态筛选。列表项含 recordNo、submitTime、手机号、盈利、分佣比例、应分佣 dueShareAmount、净额 netAmount、状态及 id。")
    @ApiResponse(responseCode = "200", description = "成功",
            content = @Content(schema = @Schema(implementation = ReferrerProfitListApiResponse.class)))
    @GetMapping("/records")
    public ApiResult<PageVo<ReferrerProfitListItemVo>> listRecords(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数，最大 100") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "可选：PENDING / APPROVED / REJECTED，不传则全部")
            @RequestParam(required = false) ProfitRecordStatus status) {
        return ApiResult.ok(profitRecordService.pageAsReferrer(SecurityUtils.requireUserId(), status, page, pageSize));
    }

    @Operation(summary = "下级收益申报详情", description = "返回申报人昵称 userNickname、策略名称 strategyName；不含 userId、referrerUserId、strategyId。")
    @ApiResponse(responseCode = "200", description = "成功",
            content = @Content(schema = @Schema(implementation = ReferrerProfitDetailApiResponse.class)))
    @GetMapping("/records/{id}")
    public ApiResult<ReferrerProfitRecordDetailVo> recordDetail(@PathVariable Long id) {
        ReferrerProfitRecordDetailVo vo = profitRecordService.getAsReferrerDetail(id, SecurityUtils.requireUserId());
        if (vo == null) {
            throw new BizException(ResultCode.NOT_FOUND, "收益申报不存在");
        }
        return ApiResult.ok(vo);
    }

    @Operation(summary = "同意（审核通过）", description = "仅申报单上的直属推荐人可操作；上上级与根用户（非直属）不可审。")
    @ApiResponse(responseCode = "200", description = "成功",
            content = @Content(schema = @Schema(implementation = ReferrerProfitAuditApiResponse.class)))
    @PostMapping("/approve")
    public ApiResult<Void> approve(@Valid @RequestBody ProfitAuditRequest req) {
        profitAuditService.approve(req.getProfitRecordId(), SecurityUtils.requireUserId(), req.getRemark(), true);
        return ApiResult.ok();
    }

    @Operation(summary = "拒绝", description = "权限同通过。")
    @ApiResponse(responseCode = "200", description = "成功",
            content = @Content(schema = @Schema(implementation = ReferrerProfitAuditApiResponse.class)))
    @PostMapping("/reject")
    public ApiResult<Void> reject(@Valid @RequestBody ProfitAuditRequest req) {
        profitAuditService.reject(req.getProfitRecordId(), SecurityUtils.requireUserId(), req.getRemark(), true);
        return ApiResult.ok();
    }
}

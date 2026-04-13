package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.dto.mt5.Mt5SnapshotReportDTO;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.Mt5SnapshotService;
import com.btg.commission.vo.Mt5SnapshotVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "MT5 账户快照")
@SecurityRequirements
@RestController
@RequestMapping("${btg.api.base-path}/mt5/snapshots")
@RequiredArgsConstructor
public class Mt5SnapshotController {

    private final Mt5SnapshotService mt5SnapshotService;

    @Operation(summary = "EA 上报 MT5 账户快照")
    @PostMapping
    public ApiResult<Void> report(@Valid @RequestBody Mt5SnapshotReportDTO dto) {
        mt5SnapshotService.reportSnapshot(dto);
        return ApiResult.ok();
    }

    @Operation(summary = "查询本人最新一条 MT5 快照", description = "从 JWT 解析当前 userId，按 user_id 查最新一条；须登录")
    @GetMapping("/latest")
    public ApiResult<Mt5SnapshotVO> latest() {
        Mt5SnapshotVO vo = mt5SnapshotService.getLatestSnapshotForUser(SecurityUtils.requireUserId());
        if (vo == null) {
            return ApiResult.fail(ResultCode.NOT_FOUND, "暂无快照记录");
        }
        return ApiResult.ok(vo);
    }
}

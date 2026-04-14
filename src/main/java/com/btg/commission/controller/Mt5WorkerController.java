package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.mt5.Mt5WorkerHeartbeatDTO;
import com.btg.commission.service.Mt5WorkerService;
import com.btg.commission.vo.mt5.AssignedMt5AccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "MT5 Worker")
@RestController
@RequestMapping("/api/mt5")
@RequiredArgsConstructor
public class Mt5WorkerController {

    private final Mt5WorkerService mt5WorkerService;

    @Operation(summary = "Worker 心跳")
    @PostMapping("/workers/heartbeat")
    public ApiResult<Void> heartbeat(@Valid @RequestBody Mt5WorkerHeartbeatDTO dto) {
        mt5WorkerService.heartbeat(dto);
        return ApiResult.ok();
    }

    @Operation(summary = "Worker 拉取已分配账号", description = "参数 workerId 实际为 workerCode，例如 worker-1")
    @GetMapping("/accounts/assigned")
    public ApiResult<List<AssignedMt5AccountVO>> assigned(@RequestParam("workerId") String workerCode) {
        return ApiResult.ok(mt5WorkerService.listAssignedAccounts(workerCode));
    }
}

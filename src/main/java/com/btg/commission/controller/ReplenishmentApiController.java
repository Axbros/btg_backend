package com.btg.commission.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.v1.ReplenishmentApplyDTO;
import com.btg.commission.dto.v1.RepayApplyDTO;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.RepayService;
import com.btg.commission.service.ReplenishmentService;
import com.btg.commission.vo.ReplenishmentApplyVO;
import com.btg.commission.vo.RepayApplyVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 玩家端：补仓 / 归仓申请。路径与现有 API 一致使用 {@code /api/v1} 前缀。
 */
@Tag(name = "补仓与归仓")
@RestController
@RequestMapping("/api/v1/replenishments")
@RequiredArgsConstructor
public class ReplenishmentApiController {

    private final ReplenishmentService replenishmentService;
    private final RepayService repayService;

    @PostMapping
    public ApiResult<Long> submitReplenishment(@Valid @RequestBody ReplenishmentApplyDTO dto) {
        return ApiResult.ok(replenishmentService.submit(SecurityUtils.requireUserId(), dto));
    }

    @GetMapping("/mine")
    public ApiResult<Page<ReplenishmentApplyVO>> mine(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(replenishmentService.pageMine(SecurityUtils.requireUserId(), page, size));
    }

    /** 当前未结清补仓（状态为审核通过或部分归还），无则 data 为 null */
    @GetMapping("/current")
    public ApiResult<ReplenishmentApplyVO> current() {
        return ApiResult.ok(replenishmentService.current(SecurityUtils.requireUserId()));
    }

    @PostMapping("/repays")
    public ApiResult<Long> submitRepay(@Valid @RequestBody RepayApplyDTO dto) {
        return ApiResult.ok(repayService.submit(SecurityUtils.requireUserId(), dto));
    }

    @GetMapping("/repays/mine")
    public ApiResult<Page<RepayApplyVO>> repaysMine(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(repayService.pageMine(SecurityUtils.requireUserId(), page, size));
    }
}

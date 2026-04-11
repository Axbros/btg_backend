package com.btg.commission.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.profit.ProfitSubmitRequest;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.ProfitRecordService;
import com.btg.commission.vo.ProfitRecordVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profits")
@RequiredArgsConstructor
public class ProfitController {

    private final ProfitRecordService profitRecordService;

    @PostMapping("/submit")
    public ApiResult<Long> submit(@Valid @RequestBody ProfitSubmitRequest req) {
        return ApiResult.ok(profitRecordService.submit(SecurityUtils.requireUserId(), req));
    }

    @GetMapping("/mine")
    public ApiResult<Page<ProfitRecordVo>> mine(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(profitRecordService.pageMine(SecurityUtils.requireUserId(), page, size));
    }
}

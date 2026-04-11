package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.service.CommissionStrategyService;
import com.btg.commission.vo.CommissionStrategyVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/strategies")
@RequiredArgsConstructor
public class StrategyController {

    private final CommissionStrategyService commissionStrategyService;

    @GetMapping
    public ApiResult<List<CommissionStrategyVo>> listEnabled() {
        return ApiResult.ok(commissionStrategyService.listEnabled());
    }
}

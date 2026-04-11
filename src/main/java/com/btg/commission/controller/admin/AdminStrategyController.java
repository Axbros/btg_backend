package com.btg.commission.controller.admin;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.strategy.StrategySaveRequest;
import com.btg.commission.service.CommissionStrategyService;
import com.btg.commission.vo.CommissionStrategyVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/strategies")
@RequiredArgsConstructor
public class AdminStrategyController {

    private final CommissionStrategyService commissionStrategyService;

    @GetMapping
    public ApiResult<List<CommissionStrategyVo>> listAll() {
        return ApiResult.ok(commissionStrategyService.listAllOrdered());
    }

    @GetMapping("/{id}")
    public ApiResult<CommissionStrategyVo> get(@PathVariable Long id) {
        return ApiResult.ok(commissionStrategyService.get(id));
    }

    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody StrategySaveRequest req) {
        return ApiResult.ok(commissionStrategyService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @Valid @RequestBody StrategySaveRequest req) {
        commissionStrategyService.update(id, req);
        return ApiResult.ok();
    }
}

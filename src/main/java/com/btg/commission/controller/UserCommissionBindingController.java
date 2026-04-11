package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.binding.BindingCreateRequest;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.UserCommissionBindingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bindings")
@RequiredArgsConstructor
public class UserCommissionBindingController {

    private final UserCommissionBindingService userCommissionBindingService;

    @PostMapping
    public ApiResult<Long> bind(@Valid @RequestBody BindingCreateRequest req) {
        Long id = userCommissionBindingService.bind(SecurityUtils.requireUserId(), req);
        return ApiResult.ok(id);
    }
}

package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.v1.ProfitConfigCreateRequest;
import com.btg.commission.dto.v1.ProfitConfigUpdateRequest;
import com.btg.commission.entity.UserProfitConfig;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.UserProfitConfigService;
import com.btg.commission.vo.SelfUnderParentProfitConfigVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "父子分润配置")
@RestController
@RequestMapping("${btg.api.base-path}/profit-configs")
@RequiredArgsConstructor
public class ProfitConfigController {

    private final UserProfitConfigService userProfitConfigService;

    @GetMapping("/my-children")
    public ApiResult<List<UserProfitConfig>> myChildren() {
        return ApiResult.ok(userProfitConfigService.listMyDirectChildrenConfigs(SecurityUtils.requireUserId()));
    }

    /** 当前登录用户（作为子级）在直属上级处的生效配置，用于利润上报前展示 */
    @Operation(summary = "本人相对上级的分润配置", description = "返回分润配置字段，并含 parentExchangeUid（上级 btg_user_profile.exchange_uid）")
    @GetMapping("/self-under-parent")
    public ApiResult<SelfUnderParentProfitConfigVo> selfUnderParent() {
        return ApiResult.ok(userProfitConfigService.findActiveForUserAsChildWithParentProfile(SecurityUtils.requireUserId()));
    }

    @PostMapping
    public ApiResult<UserProfitConfig> create(@Valid @RequestBody ProfitConfigCreateRequest req) {
        return ApiResult.ok(userProfitConfigService.create(
                SecurityUtils.requireUserId(), req.getChildUserId(), req.getChildProfitRatio()));
    }

    @PutMapping("/{id}")
    public ApiResult<UserProfitConfig> update(@PathVariable Long id, @Valid @RequestBody ProfitConfigUpdateRequest req) {
        return ApiResult.ok(userProfitConfigService.updateById(
                id, SecurityUtils.requireUserId(), req.getChildProfitRatio()));
    }
}

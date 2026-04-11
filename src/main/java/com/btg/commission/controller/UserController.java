package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.dto.profile.ProfileCompleteRequest;
import com.btg.commission.service.UserProfileService;
import com.btg.commission.service.UserService;
import com.btg.commission.vo.UserProfileVo;
import com.btg.commission.vo.PageVo;
import com.btg.commission.vo.TeamMemberBriefVo;
import com.btg.commission.vo.UserDetailVo;
import com.btg.commission.vo.UserMeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserProfileService userProfileService;

    @Operation(summary = "查询资料")
    @GetMapping("/profile")
    public ApiResult<UserProfileVo> profile() {
        return ApiResult.ok(userProfileService.getProfile(SecurityUtils.requireUserId()));
    }

    @Operation(summary = "更新资料")
    @PutMapping("/profile")
    public ApiResult<UserProfileVo> updateProfile(@Valid @RequestBody ProfileCompleteRequest req) {
        return ApiResult.ok(userProfileService.completeProfile(SecurityUtils.requireUserId(), req));
    }

    @Operation(summary = "当前用户（与 GET /api/v1/me 相同）", description = "含直属上级展示名 referrerNickname（昵称为空时为上级手机号）")
    @GetMapping("/me")
    public ApiResult<UserMeVo> me() {
        UserMeVo vo = userService.me(SecurityUtils.requireUserId());
        if (vo == null) {
            throw new BizException(ResultCode.NOT_FOUND, "user not found");
        }
        return ApiResult.ok(vo);
    }

    @Operation(summary = "直属下级（分页）")
    @GetMapping("/team/direct")
    public ApiResult<PageVo<TeamMemberBriefVo>> direct(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数，最大 100") @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResult.ok(userService.pageDirectChildren(SecurityUtils.requireUserId(), page, pageSize));
    }

    @Operation(summary = "全部下级（分页）")
    @GetMapping("/team/descendants")
    public ApiResult<PageVo<TeamMemberBriefVo>> descendants(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数，最大 100") @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResult.ok(userService.pageAllDescendants(SecurityUtils.requireUserId(), page, pageSize));
    }

    @Operation(summary = "按用户ID查看用户", description = "返回 user、profile；childLineProfitRatio 为当前登录用户视角下该用户所在直属分支的子级总利润占比（无则为 null）")
    @GetMapping("/{id}")
    public ApiResult<UserDetailVo> userById(@PathVariable Long id) {
        UserDetailVo vo = userService.getDetailById(id, SecurityUtils.requireUserId());
        if (vo == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return ApiResult.ok(vo);
    }
}

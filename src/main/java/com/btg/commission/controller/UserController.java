package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.UserService;
import com.btg.commission.vo.PageVo;
import com.btg.commission.vo.TeamMemberBriefVo;
import com.btg.commission.vo.UserDetailVo;
import com.btg.commission.vo.UserMeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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

    @Operation(summary = "按用户ID查看用户", description = "返回 user、完整 user_profile（无则 profile 为 null），以及与直属上级有效绑定的 strategyId、strategyName、commissionRate")
    @GetMapping("/{id}")
    public ApiResult<UserDetailVo> userById(@PathVariable Long id) {
        UserDetailVo vo = userService.getDetailById(id);
        if (vo == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return ApiResult.ok(vo);
    }
}

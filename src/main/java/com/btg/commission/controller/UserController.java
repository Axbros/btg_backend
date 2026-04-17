package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.dto.profile.ProfileCompleteRequest;
import com.btg.commission.dto.user.QualificationResubmitRequest;
import com.btg.commission.service.UserProfileService;
import com.btg.commission.service.UserQualificationService;
import com.btg.commission.service.UserService;
import com.btg.commission.vo.UserProfileVo;
import com.btg.commission.vo.TeamMemberTreeVo;
import com.btg.commission.vo.UserDetailVo;
import com.btg.commission.vo.UserMeVo;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("${btg.api.base-path}/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserProfileService userProfileService;
    private final UserQualificationService userQualificationService;

    @Operation(summary = "查询资料")
    @GetMapping("/profile")
    public ApiResult<UserProfileVo> profile() {
        return ApiResult.ok(userProfileService.getProfile(SecurityUtils.requireUserId()));
    }

    @Operation(summary = "更新资料", description = "status=-1 提交后变为 0 待审核；status=0 审核中仍可修改并再次提交；status=1 已通过仍可修改资料且保持已通过，不改为待审。手机号不可改：请求体 mobile 若填写须与账号一致，否则拒绝。walletName、walletAddress 必填；真实姓名与身份证号非必填")
    @PutMapping("/profile")
    public ApiResult<UserProfileVo> updateProfile(@Valid @RequestBody ProfileCompleteRequest req) {
        return ApiResult.ok(userProfileService.completeProfile(SecurityUtils.requireUserId(), req));
    }

    @Operation(summary = "重新提交系统管理员资格审核", description = "仅当资格状态为「已拒绝」且资料必填项齐全时可调用；提交后回到待审")
    @PostMapping("/qualification/resubmit")
    public ApiResult<Void> resubmitQualification(@RequestBody(required = false) @Valid QualificationResubmitRequest req) {
        String remark = req == null ? null : req.getRemark();
        userQualificationService.resubmitQualification(SecurityUtils.requireUserId(), remark);
        return ApiResult.ok();
    }

    @Operation(summary = "当前用户（与 GET …/me 相同）", description = "含直属上级展示名 referrerNickname（昵称为空时为上级手机号）；前缀见 btg.api.base-path")
    @GetMapping("/me")
    public ApiResult<UserMeVo> me() {
        UserMeVo vo = userService.me(SecurityUtils.requireUserId());
        if (vo == null) {
            throw new BizException(ResultCode.NOT_FOUND, "user not found");
        }
        return ApiResult.ok(vo);
    }

    @Operation(summary = "全部下级（树）", description = "根节点为直属下级，children 为多级下级；节点含 id、nickname、status")
    @GetMapping("/team/descendants")
    public ApiResult<List<TeamMemberTreeVo>> descendants() {
        return ApiResult.ok(userService.treeDescendants(SecurityUtils.requireUserId()));
    }

    @Operation(summary = "按用户ID查看用户", description = "返回 user、profile；childLineProfitRatio 为当前登录用户视角下该用户所在直属分支的子级总利润占比（无则为 null）；maxAssignableChildProfitRatio 为当前用户调整该分支子级占比时可配置的上限 0～1（无则为 null）")
    @GetMapping("/{id}")
    public ApiResult<UserDetailVo> userById(@PathVariable Long id) {
        UserDetailVo vo = userService.getDetailById(id, SecurityUtils.requireUserId());
        if (vo == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return ApiResult.ok(vo);
    }
}

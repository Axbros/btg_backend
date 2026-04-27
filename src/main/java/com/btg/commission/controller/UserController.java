package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.dto.profile.ProfileCompleteRequest;
import com.btg.commission.dto.user.QualificationResubmitRequest;
import com.btg.commission.service.TeamStatsService;
import com.btg.commission.service.UserProfileService;
import com.btg.commission.service.UserQualificationService;
import com.btg.commission.service.UserService;
import com.btg.commission.vo.TeamDescendantsViewVO;
import com.btg.commission.vo.UserProfileVo;
import com.btg.commission.vo.UserDetailVo;
import com.btg.commission.vo.TeamStatsVo;
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

@RestController
@RequestMapping("${btg.api.base-path}/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TeamStatsService teamStatsService;
    private final UserProfileService userProfileService;
    private final UserQualificationService userQualificationService;

    @Operation(summary = "查询资料")
    @GetMapping("/profile")
    public ApiResult<UserProfileVo> profile() {
        return ApiResult.ok(userProfileService.getProfile(SecurityUtils.requireUserId()));
    }

    @Operation(summary = "更新资料", description = "若 user_profile 已存在且 qualification_status=待系统管理员审核（1），则禁止修改资料直至审核结束；首次创建资料行、或资格为已拒绝/已通过时仍可按规则更新。status=-1 提交后变为 0 待上级审核；资格已拒绝可改资料并自动回到待审（见接口说明）。手机号不可改。walletName、walletAddress 必填；真实姓名与身份证号非必填")
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

    @Operation(
            summary = "团队统计与全部下级（树）",
            description = "含直属/全团队人数与下级树。原 GET …/me/team-stats 已合并至此，请不要再调用 /me/team-stats")
    @GetMapping("/team/descendants")
    public ApiResult<TeamDescendantsViewVO> descendants() {
        Long userId = SecurityUtils.requireUserId();
        TeamStatsVo s = teamStatsService.stats(userId);
        return ApiResult.ok(TeamDescendantsViewVO.builder()
                .directCount(s.getDirectCount())
                .allDescendantCount(s.getAllDescendantCount())
                .descendants(userService.treeDescendants(userId))
                .build());
    }

    @Operation(summary = "按用户ID查看用户", description = "返回 user、profile；viewerProfitConfig 含当前上下文 ACTIVE 边的兜底/不兜底比例、模式，以及当前登录用户为下级配置两档比例时各自允许的上限 maxAssignableChildGuaranteeRatio / maxAssignableChildNonGuaranteeRatio（无 ACTIVE 边时整体为 null）")
    @GetMapping("/{id}")
    public ApiResult<UserDetailVo> userById(@PathVariable Long id) {
        UserDetailVo vo = userService.getDetailById(id, SecurityUtils.requireUserId());
        if (vo == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return ApiResult.ok(vo);
    }
}

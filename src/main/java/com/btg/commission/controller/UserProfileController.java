package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.dto.profile.ProfileCompleteRequest;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.UserProfileService;
import com.btg.commission.vo.UserProfileVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户资料")
@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "完善资料", description = "更新用户名（昵称）、实名与交易相关信息；身份证正反面、人脸照片 URL 可选（不传则不覆盖原值，传空串可清空）。提交后 KYC 将进入待审核（已通过审核的不改 KYC 状态）。")
    @PutMapping
    public ApiResult<UserProfileVo> complete(@Valid @RequestBody ProfileCompleteRequest req) {
        return ApiResult.ok(userProfileService.completeProfile(SecurityUtils.requireUserId(), req));
    }
}

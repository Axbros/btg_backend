package com.btg.commission.controller;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.security.SecurityUtils;
import com.btg.commission.service.UserService;
import com.btg.commission.vo.UserPickerOptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端用户列表类接口（与 {@link AdminUserQualificationController} 同前缀，路径不冲突）。
 */
@Tag(name = "管理-用户")
@RestController
@RequestMapping("${btg.api.base-path}/admin/users")
@RequiredArgsConstructor
public class AdminUserListController {

    private final UserService userService;

    @Operation(
            summary = "全部用户 id + 昵称（指派下拉）",
            description = "未删除用户全量；仅 id、nickname。供补仓转派资方等场景快速选择；根用户仅系统管理员可操作，具体业务仍以接口校验为准。")
    @GetMapping("/picker-options")
    public ApiResult<List<UserPickerOptionVO>> pickerOptions() {
        SecurityUtils.requireRootUser();
        return ApiResult.ok(userService.listAllUsersForPicker());
    }
}

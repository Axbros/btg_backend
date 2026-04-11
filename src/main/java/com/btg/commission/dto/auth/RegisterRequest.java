package com.btg.commission.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    @Pattern(regexp = "^1\\d{10}$", message = "invalid mobile")
    private String mobile;

    @NotBlank
    @Size(min = 6, max = 64)
    private String password;

    /** 可选；为空时自动挂到系统根用户下 */
    private String invitationCode;
}

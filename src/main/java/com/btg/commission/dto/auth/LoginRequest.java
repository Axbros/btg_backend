package com.btg.commission.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    private String mobile;

    @NotBlank
    private String password;
}

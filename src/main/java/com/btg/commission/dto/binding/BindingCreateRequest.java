package com.btg.commission.dto.binding;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BindingCreateRequest {

    @NotNull
    private Long childUserId;

    @NotNull
    private Long strategyId;
}

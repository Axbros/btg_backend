package com.btg.commission.dto.v1;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReplenishmentAssignCapitalRequest {

    @NotNull(message = "资方执行人不能为空")
    private Long capitalUserId;

    @Size(max = 500)
    private String remark;
}

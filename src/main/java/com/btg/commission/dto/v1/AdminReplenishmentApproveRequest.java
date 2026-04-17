package com.btg.commission.dto.v1;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminReplenishmentApproveRequest {

    @Size(max = 500)
    private String remark;
}

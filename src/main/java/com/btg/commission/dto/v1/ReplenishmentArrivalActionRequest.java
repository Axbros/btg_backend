package com.btg.commission.dto.v1;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReplenishmentArrivalActionRequest {

    @Size(max = 500)
    private String remark;
}

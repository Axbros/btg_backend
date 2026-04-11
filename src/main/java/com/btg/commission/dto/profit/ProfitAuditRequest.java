package com.btg.commission.dto.profit;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProfitAuditRequest {

    @NotNull
    private Long profitRecordId;

    private String remark;
}

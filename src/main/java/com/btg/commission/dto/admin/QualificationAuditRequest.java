package com.btg.commission.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QualificationAuditRequest {

    @Size(max = 500)
    private String remark;
}

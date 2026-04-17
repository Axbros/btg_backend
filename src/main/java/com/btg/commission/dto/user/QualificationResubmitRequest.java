package com.btg.commission.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QualificationResubmitRequest {

    @Size(max = 500)
    private String remark;
}

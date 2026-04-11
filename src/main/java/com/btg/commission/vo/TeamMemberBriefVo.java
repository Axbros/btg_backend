package com.btg.commission.vo;

import com.btg.commission.enums.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberBriefVo {

    private Long id;
    private String mobile;
    /** 无资料行或未设置时为 null */
    private KycStatus kycStatus;
}

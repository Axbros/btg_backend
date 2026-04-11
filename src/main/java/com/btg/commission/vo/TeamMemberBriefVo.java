package com.btg.commission.vo;

import com.btg.commission.enums.UserStatus;
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
    /** {@code btg_user.status}：-1 待完善；0 待审核；1 正常 */
    private UserStatus status;
}

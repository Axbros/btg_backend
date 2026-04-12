package com.btg.commission.vo;

import com.btg.commission.enums.UserStatus;
import lombok.Data;

/**
 * 查询结果行，用于内存组装 {@link TeamMemberTreeVo}。
 */
@Data
public class TeamMemberTreeRow {

    private Long id;
    private Long referrerUserId;
    private String nickname;
    private UserStatus status;
}

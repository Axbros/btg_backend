package com.btg.commission.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeamStatsVo {

    private int directCount;
    private int allDescendantCount;
}

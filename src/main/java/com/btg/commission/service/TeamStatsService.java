package com.btg.commission.service;

import com.btg.commission.vo.TeamStatsVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamStatsService {

    private final UserService userService;

    public TeamStatsVo stats(Long userId) {
        long direct = userService.countDirectChildren(userId);
        long all = userService.countAllDescendants(userId);
        return TeamStatsVo.builder()
                .directCount(toIntCapped(direct))
                .allDescendantCount(toIntCapped(all))
                .build();
    }

    private static int toIntCapped(long v) {
        if (v > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) v;
    }
}

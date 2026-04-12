package com.btg.commission.service;

import com.btg.commission.vo.PendingSummaryVO;

public interface DashboardService {

    /**
     * 当前用户待办条数汇总；金额不返回。
     * 补仓/归仓待审仅 {@code btg_user.is_root = true} 的用户统计，否则对应项为 0。
     */
    PendingSummaryVO getPendingSummary(Long currentUserId);
}

package com.btg.commission.service;

import com.btg.commission.vo.PendingSummaryVO;
import com.btg.commission.vo.flow.DashboardTodoItemVO;

import java.util.List;

public interface DashboardService {

    /**
     * 当前用户待办条数汇总；金额不返回。
     * 含本人待支付给上级的结算单（付款人视角待提交/待审）。
     * 补仓/归仓待审仅 {@code btg_user.is_root = true} 的用户统计，否则对应项为 0。
     */
    PendingSummaryVO getPendingSummary(Long currentUserId);

    /** 首页待办/待处理统一列表（条数较多时由各查询 LIMIT 控制） */
    List<DashboardTodoItemVO> listTodoItems(Long currentUserId);
}

package com.btg.commission.service;

import com.btg.commission.vo.flow.ProfitFlowDetailVO;

/**
 * 利润分润链路详情查询（按 root_report_id + 当前用户权限与数据范围）。
 */
public interface ProfitFlowDetailQuery {

    ProfitFlowDetailVO getProfitFlowDetailByRootReportId(Long rootReportId, Long currentUserId);
}

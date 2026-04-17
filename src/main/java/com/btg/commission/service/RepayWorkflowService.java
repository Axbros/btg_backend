package com.btg.commission.service;

import com.btg.commission.dto.v1.RepayApplyDTO;
import com.btg.commission.dto.v1.RepayResubmitRequest;

/**
 * 归仓申请与补仓主单联动的写路径（提交 / 重提 / 资方审核）。
 */
public interface RepayWorkflowService {

    Long submitRepay(Long currentUserId, RepayApplyDTO dto);

    void resubmitRepay(Long currentUserId, Long repayId, RepayResubmitRequest dto);

    void approveRepay(Long currentUserId, Long repayId, String remark);

    void rejectRepay(Long currentUserId, Long repayId, String remark);
}

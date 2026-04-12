package com.btg.commission.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.dto.v1.RepayApplyDTO;
import com.btg.commission.vo.RepayApplyVO;
import com.btg.commission.vo.RepayPendingBriefVO;

public interface RepayService {

    Long submit(Long userId, RepayApplyDTO dto);

    Page<RepayPendingBriefVO> pageMine(Long userId, long page, long size);

    Page<RepayPendingBriefVO> pagePendingForAdmin(long page, long size);

    /** 本人归仓详情（含 replenishmentApply）；非本人返回 403 */
    RepayApplyVO getRepayDetailForUser(Long userId, Long repayApplyId);

    /** 资方查看单条归仓详情（含申请人、补仓单完整信息等） */
    RepayApplyVO getAdminRepayDetail(Long repayApplyId);

    void approveForAdmin(Long repayApplyId, Long adminUserId, String remark);

    void rejectForAdmin(Long repayApplyId, Long adminUserId, String remark);
}

package com.btg.commission.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.dto.v1.RepayApplyDTO;
import com.btg.commission.vo.RepayApplyVO;
import com.btg.commission.vo.RepayPendingBriefVO;

public interface RepayService {

    Long submit(Long userId, RepayApplyDTO dto);

    Page<RepayPendingBriefVO> pageMine(Long userId, long page, long size);

    /** 本人下级链（不含本人）的归仓申请分页，结构与资方详情 VO 一致（含嵌套补仓单） */
    Page<RepayApplyVO> pageTeamDescendantRepays(Long viewerUserId, long page, long size);

    Page<RepayPendingBriefVO> pagePendingForAdmin(long page, long size);

    /** 本人归仓详情（含 replenishmentApply）；本人或上级链团队长可查看 */
    RepayApplyVO getRepayDetailForUser(Long viewerUserId, Long repayApplyId);

    /** 资方查看单条归仓详情（含申请人、补仓单完整信息等） */
    RepayApplyVO getAdminRepayDetail(Long repayApplyId);

    void approveForAdmin(Long repayApplyId, Long adminUserId, String remark);

    void rejectForAdmin(Long repayApplyId, Long adminUserId, String remark);
}

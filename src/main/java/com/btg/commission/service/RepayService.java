package com.btg.commission.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.dto.v1.RepayApplyDTO;
import com.btg.commission.vo.RepayApplyVO;
import com.btg.commission.vo.RepayPendingBriefVO;
import com.btg.commission.vo.RepayableReplenishmentVO;
import com.btg.commission.vo.ReplenishmentTeamItemVO;

import java.util.List;

public interface RepayService {

    Long submit(Long userId, RepayApplyDTO dto);

    /** 当前用户可归仓的补仓单（status∈{2,4}、remaining>0、未删除） */
    List<RepayableReplenishmentVO> listRepayableReplenishments(Long currentUserId);

    Page<RepayPendingBriefVO> pageMine(Long userId, long page, long size);

    /** 本人下级链（不含本人）的归仓申请分页；每条与 GET …/replenishments/team 相同字段（id、status、nickname、mobile、replenishAmount=repay_amount） */
    Page<ReplenishmentTeamItemVO> pageTeamDescendantRepays(Long viewerUserId, long page, long size);

    Page<RepayPendingBriefVO> pagePendingForAdmin(long page, long size);

    /** 本人归仓详情（含 replenishmentApply）；本人或上级链团队长可查看 */
    RepayApplyVO getRepayDetailForUser(Long viewerUserId, Long repayApplyId);

    /** 资方查看单条归仓详情（含申请人、补仓单完整信息等） */
    RepayApplyVO getAdminRepayDetail(Long repayApplyId);

    void approveForAdmin(Long repayApplyId, Long adminUserId, String remark);

    void rejectForAdmin(Long repayApplyId, Long adminUserId, String remark);
}

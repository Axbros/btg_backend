package com.btg.commission.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.dto.v1.RepayApplyDTO;
import com.btg.commission.dto.v1.RepayResubmitRequest;
import com.btg.commission.vo.RepayApplyVO;
import com.btg.commission.vo.RepayPendingBriefVO;
import com.btg.commission.vo.RepayableReplenishmentVO;
import com.btg.commission.vo.ReplenishmentTeamItemVO;
import com.btg.commission.vo.flow.RepayApplyFlowDetailVO;

import java.util.List;

public interface RepayService {

    Long submit(Long userId, RepayApplyDTO dto);

    /** 当前用户可归仓的补仓单（SUCCESS、remaining>0、已指定资方执行人、未删除） */
    List<RepayableReplenishmentVO> listRepayableReplenishments(Long currentUserId);

    Page<RepayPendingBriefVO> pageMine(Long userId, long page, long size);

    /** 本人下级链（不含本人）的归仓申请分页；每条与 GET …/replenishments/team 相同字段（id、status、nickname、mobile、replenishAmount=repay_amount） */
    Page<ReplenishmentTeamItemVO> pageTeamDescendantRepays(Long viewerUserId, long page, long size);

    /** 资方执行人：待本人审核的归仓申请 */
    Page<RepayPendingBriefVO> pagePendingReviewForCapital(Long capitalUserId, long page, long size);

    /** 本人归仓详情（含 replenishmentApply）；本人、补仓执行方、上级链、系统管理员可查看 */
    RepayApplyVO getRepayDetailForUser(Long viewerUserId, Long repayApplyId);

    /** 管理员查看单条归仓详情（含申请人、补仓单完整信息等） */
    RepayApplyVO getAdminRepayDetail(Long repayApplyId);

    void approveRepay(Long capitalUserId, Long repayId, String remark);

    void rejectRepay(Long capitalUserId, Long repayId, String remark);

    void resubmit(Long userId, Long repayApplyId, RepayResubmitRequest req);

    RepayApplyFlowDetailVO flowDetail(Long viewerUserId, Long repayApplyId);
}

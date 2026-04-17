package com.btg.commission.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.dto.v1.ReplenishmentApplyDTO;
import com.btg.commission.dto.v1.ReplenishmentAssignCapitalRequest;
import com.btg.commission.dto.v1.ReplenishmentCapitalSubmitRequest;
import com.btg.commission.dto.v1.ReplenishmentResubmitRequest;
import com.btg.commission.entity.BtgReplenishmentApply;
import com.btg.commission.vo.ReplenishmentApplyBriefVO;
import com.btg.commission.vo.ReplenishmentApplyDetailVO;
import com.btg.commission.vo.ReplenishmentApplyVO;
import com.btg.commission.vo.ReplenishmentPendingBriefVO;
import com.btg.commission.vo.ReplenishmentTeamItemVO;
import com.btg.commission.vo.flow.ReplenishmentApplyFlowDetailVO;

public interface ReplenishmentService {

    Long submit(Long userId, ReplenishmentApplyDTO dto);

    Page<ReplenishmentApplyBriefVO> pageMine(Long userId, long page, long size);

    /** 资方执行人：待提交 / 被退回的补仓单 */
    Page<ReplenishmentApplyBriefVO> pageAssignedToMe(Long capitalUserId, long page, long size);

    /** 本人下级链（不含本人）的补仓申请分页；每条 id、status、nickname、mobile、replenishAmount */
    Page<ReplenishmentTeamItemVO> pageTeamDescendantApplies(Long viewerUserId, long page, long size);

    /** 本人或团队长或资方执行人查看补仓详情；含审核通过的归仓记录 */
    ReplenishmentApplyDetailVO getReplenishmentDetailForUser(Long viewerUserId, Long applyId);

    /** 当前未结清补仓（SUCCESS 且剩余应还 &gt; 0），无则返回 null */
    ReplenishmentApplyVO current(Long userId);

    /** 管理员：待管理员审核 */
    Page<ReplenishmentPendingBriefVO> pagePendingForAdmin(long page, long size);

    /** 管理员：全部补仓单分页 */
    Page<ReplenishmentApplyVO> pageAllForAdmin(long page, long size);

    ReplenishmentApplyVO getReplenishmentDetailForAdmin(Long applyId);

    void approveByAdmin(Long applyId, Long adminUserId, String remark);

    void rejectByAdmin(Long applyId, Long adminUserId, String remark);

    void assignCapital(Long applyId, Long adminUserId, ReplenishmentAssignCapitalRequest req);

    void capitalSubmit(Long capitalUserId, Long applyId, ReplenishmentCapitalSubmitRequest dto);

    void confirmArrival(Long applicantUserId, Long applyId, String remark);

    void rejectArrival(Long applicantUserId, Long applyId, String remark);

    /** 申请人：管理员拒绝后重新提交，或资料修正后重新进入待审 */
    void resubmit(Long userId, Long applyId, ReplenishmentResubmitRequest req);

    ReplenishmentApplyFlowDetailVO flowDetail(Long viewerUserId, Long applyId);

    ReplenishmentApplyVO toApplyVo(BtgReplenishmentApply entity);
}

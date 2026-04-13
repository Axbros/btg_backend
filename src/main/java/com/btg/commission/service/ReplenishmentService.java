package com.btg.commission.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.dto.v1.ReplenishmentApplyDTO;
import com.btg.commission.dto.v1.ReplenishmentApproveDTO;
import com.btg.commission.entity.BtgReplenishmentApply;
import com.btg.commission.vo.ReplenishmentApplyBriefVO;
import com.btg.commission.vo.ReplenishmentApplyDetailVO;
import com.btg.commission.vo.ReplenishmentApplyVO;

public interface ReplenishmentService {

    Long submit(Long userId, ReplenishmentApplyDTO dto);

    Page<ReplenishmentApplyBriefVO> pageMine(Long userId, long page, long size);

    /** 本人下级链（不含本人）的补仓申请分页，含申请人钱包等展示字段 */
    Page<ReplenishmentApplyVO> pageTeamDescendantApplies(Long viewerUserId, long page, long size);

    /** 本人或团队长查看补仓详情；含审核通过的归仓记录 */
    ReplenishmentApplyDetailVO getReplenishmentDetailForUser(Long viewerUserId, Long applyId);

    /** 当前未结清补仓（审核通过或部分归还），无则返回 null */
    ReplenishmentApplyVO current(Long userId);

    Page<ReplenishmentApplyVO> pagePendingForAdmin(long page, long size);

    /** 资方受理：待审核(1) → 待上传凭证(7) */
    void acceptForAdmin(Long applyId, Long adminUserId);

    /** 资方终审确认：状态 8 → 2；凭证与备注须在「上传资方凭证」步骤已写入 */
    void approveForAdmin(Long applyId, Long adminUserId);

    void rejectForAdmin(Long applyId, Long adminUserId, String remark);

    /** 资方上传转账凭证与备注：状态 7 → 8 */
    void submitCapitalVoucherForAdmin(Long adminUserId, Long applyId, ReplenishmentApproveDTO dto);

    /** 补仓单实体转 VO，供归仓详情等复用 */
    ReplenishmentApplyVO toApplyVo(BtgReplenishmentApply entity);
}

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

    /** 本人补仓详情；非本人 403；含审核通过的归仓记录 */
    ReplenishmentApplyDetailVO getReplenishmentDetailForUser(Long userId, Long applyId);

    /** 当前未结清补仓（审核通过或部分归还），无则返回 null */
    ReplenishmentApplyVO current(Long userId);

    Page<ReplenishmentApplyVO> pagePendingForAdmin(long page, long size);

    void approveForAdmin(Long applyId, Long adminUserId, ReplenishmentApproveDTO dto);

    void rejectForAdmin(Long applyId, Long adminUserId, String remark);

    /** 补仓单实体转 VO，供归仓详情等复用 */
    ReplenishmentApplyVO toApplyVo(BtgReplenishmentApply entity);
}

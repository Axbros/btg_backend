package com.btg.commission.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.dto.v1.ReplenishmentApplyDTO;
import com.btg.commission.dto.v1.ReplenishmentApproveDTO;
import com.btg.commission.vo.ReplenishmentApplyVO;

public interface ReplenishmentService {

    Long submit(Long userId, ReplenishmentApplyDTO dto);

    Page<ReplenishmentApplyVO> pageMine(Long userId, long page, long size);

    /** 当前未结清补仓（审核通过或部分归还），无则返回 null */
    ReplenishmentApplyVO current(Long userId);

    Page<ReplenishmentApplyVO> pagePendingForAdmin(long page, long size);

    void approveForAdmin(Long applyId, Long adminUserId, ReplenishmentApproveDTO dto);

    void rejectForAdmin(Long applyId, Long adminUserId, String remark);
}

package com.btg.commission.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.dto.v1.RepayApplyDTO;
import com.btg.commission.vo.RepayApplyVO;

public interface RepayService {

    Long submit(Long userId, RepayApplyDTO dto);

    Page<RepayApplyVO> pageMine(Long userId, long page, long size);

    Page<RepayApplyVO> pagePendingForAdmin(long page, long size);

    void approveForAdmin(Long repayApplyId, Long adminUserId, String remark);

    void rejectForAdmin(Long repayApplyId, Long adminUserId, String remark);
}

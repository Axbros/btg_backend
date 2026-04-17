package com.btg.commission.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.vo.PendingQualificationUserVO;

public interface UserQualificationService {

    Page<PendingQualificationUserVO> pagePendingQualification(long page, long size);

    void approveQualification(Long userId, Long operatorUserId, String remark);

    void rejectQualification(Long userId, Long operatorUserId, String remark);

    /** 当前用户资格为「已拒绝」时重新进入待审 */
    void resubmitQualification(Long currentUserId, String remark);
}

package com.btg.commission.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.vo.PendingQualificationUserVO;

public interface UserQualificationService {

    Page<PendingQualificationUserVO> pagePendingQualification(Long operatorUserId, long page, long size);

    void approveQualification(Long userId, Long operatorUserId, String remark);

    void rejectQualification(Long userId, Long operatorUserId, String remark);

    /** 当前用户资格为「已拒绝」时重新进入待审 */
    void resubmitQualification(Long currentUserId, String remark);

    /**
     * 与 {@link #resubmitQualification} 相同的资料齐备校验（身份证、人脸、交易信息、底仓本金等）。
     */
    void assertProfileCompleteForQualificationResubmit(UserProfile profile);

    /**
     * 将 {@code qualification_status} 从「拒绝」改为「待系统管理员审核」、清空审核信息并递增提交次数、写审计。
     * 调用方须保证 {@link UserProfile#getQualificationStatus()} 为 {@link com.btg.commission.enums.QualificationStatusEnum#REJECTED}
     * 且已调用 {@link #assertProfileCompleteForQualificationResubmit}。
     */
    void applyQualificationRejectedToPendingForResubmit(Long userId, UserProfile profile, String remark);
}

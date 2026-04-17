package com.btg.commission.service;

import com.btg.commission.dto.v1.AdminReplenishmentApproveRequest;
import com.btg.commission.dto.v1.ReplenishmentCapitalSubmitRequest;

public interface ReplenishmentWorkflowService {

    void approveByAdmin(Long applyId, Long adminUserId, AdminReplenishmentApproveRequest req);

    void rejectByAdmin(Long applyId, Long adminUserId, String remark);

    void assignCapital(Long applyId, Long adminUserId, Long capitalUserId, String remark);

    /** 资方执行人拒绝执行：退回待管理员重新转派（仅同意/拒绝中的「拒绝」） */
    void rejectCapitalAssignment(Long applyId, Long capitalUserId, String remark);

    void capitalSubmit(Long applyId, Long capitalUserId, ReplenishmentCapitalSubmitRequest dto);

    void confirmArrival(Long applyId, Long applicantUserId, String remark);

    void rejectArrival(Long applyId, Long applicantUserId, String remark);
}

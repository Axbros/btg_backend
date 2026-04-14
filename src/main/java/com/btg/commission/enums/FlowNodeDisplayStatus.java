package com.btg.commission.enums;

import lombok.Getter;

/**
 * 链路节点展示用状态（给前端映射文案：待提交 / 待审核 / 已通过 / 已拒绝 / 已退回待修改）
 */
@Getter
public enum FlowNodeDisplayStatus {

    PENDING_SUBMIT,
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    RETURNED_FOR_EDIT,
    IN_PROGRESS,
    SKIPPED
}

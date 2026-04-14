package com.btg.commission.enums;

import lombok.Getter;

/**
 * 流转节点角色（落库为字符串，便于扩展）
 */
@Getter
public enum FlowNodeRole {

    APPLICANT,
    DIRECT_PARENT,
    UPLINE,
    CAPITAL,
    ROOT,
    SYSTEM
}

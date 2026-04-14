package com.btg.commission.vo.flow;

import com.btg.commission.enums.FlowNodeDisplayStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "流转链路节点（单条日志或聚合后的节点视图）")
public class BusinessFlowNodeVO {

    private Long nodeUserId;
    private String nodeName;
    /** {@link com.btg.commission.enums.FlowNodeRole#name()} */
    private String nodeRole;
    /** {@link com.btg.commission.enums.FlowAction#name()} */
    private String action;
    private FlowNodeDisplayStatus displayStatus;
    private String remark;
    private LocalDateTime operateTime;
    private Long operatorUserId;
    private String operatorName;
    private Integer versionNo;
}

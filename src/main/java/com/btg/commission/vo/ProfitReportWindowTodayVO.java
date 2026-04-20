package com.btg.commission.vo;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Value
@Builder
public class ProfitReportWindowTodayVO {

    LocalDate businessDate;
    boolean recordExists;
    /** 当日已开始且未结束，与成员能否上报一致 */
    boolean acceptingProfitReport;

    LocalDateTime openedAt;
    LocalDateTime closedAt;
}

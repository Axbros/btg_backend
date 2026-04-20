package com.btg.commission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btg.commission.entity.ProfitReportWindow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

public interface ProfitReportWindowMapper extends BaseMapper<ProfitReportWindow> {

    @Select("SELECT * FROM btg_profit_report_window WHERE business_date = #{businessDate} AND deleted_at IS NULL FOR UPDATE")
    ProfitReportWindow selectByBusinessDateForUpdate(@Param("businessDate") LocalDate businessDate);
}

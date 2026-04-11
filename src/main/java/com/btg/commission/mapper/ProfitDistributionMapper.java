package com.btg.commission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btg.commission.entity.ProfitDistribution;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

public interface ProfitDistributionMapper extends BaseMapper<ProfitDistribution> {

    @Select("""
            SELECT COALESCE(SUM(income_amount), 0)
            FROM btg_profit_distribution
            WHERE beneficiary_user_id = #{userId}
              AND deleted_at IS NULL
            """)
    BigDecimal sumIncomeByBeneficiary(@Param("userId") Long userId);
}

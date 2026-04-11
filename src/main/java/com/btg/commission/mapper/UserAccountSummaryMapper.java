package com.btg.commission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btg.commission.entity.UserAccountSummary;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

public interface UserAccountSummaryMapper extends BaseMapper<UserAccountSummary> {

    @Select("SELECT * FROM btg_user_account_summary WHERE user_id = #{userId} FOR UPDATE")
    UserAccountSummary selectByUserIdForUpdate(@Param("userId") Long userId);

    @Update("""
            UPDATE btg_user_account_summary
            SET pending_commission_out_amount = IFNULL(pending_commission_out_amount, 0) + #{delta},
                updated_at = NOW()
            WHERE user_id = #{userId}
            """)
    int addPendingCommissionOut(@Param("userId") Long userId, @Param("delta") BigDecimal delta);

    @Update("""
            UPDATE btg_user_account_summary
            SET pending_commission_in_amount = IFNULL(pending_commission_in_amount, 0) + #{delta},
                updated_at = NOW()
            WHERE user_id = #{userId}
            """)
    int addPendingCommissionIn(@Param("userId") Long userId, @Param("delta") BigDecimal delta);

    @Update("""
            UPDATE btg_user_account_summary
            SET pending_commission_out_amount = IFNULL(pending_commission_out_amount, 0) - #{delta},
                updated_at = NOW()
            WHERE user_id = #{userId}
            """)
    int subtractPendingCommissionOut(@Param("userId") Long userId, @Param("delta") BigDecimal delta);

    @Update("""
            UPDATE btg_user_account_summary
            SET pending_commission_in_amount = IFNULL(pending_commission_in_amount, 0) - #{delta},
                updated_at = NOW()
            WHERE user_id = #{userId}
            """)
    int subtractPendingCommissionIn(@Param("userId") Long userId, @Param("delta") BigDecimal delta);
}

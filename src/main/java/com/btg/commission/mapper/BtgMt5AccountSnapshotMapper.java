package com.btg.commission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btg.commission.entity.BtgMt5AccountSnapshot;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface BtgMt5AccountSnapshotMapper extends BaseMapper<BtgMt5AccountSnapshot> {

    /**
     * 手写 SQL 须显式过滤软删除；与 MybatisPlusConfig 说明一致。
     */
    @Select("""
            SELECT *
            FROM btg_mt5_account_snapshot
            WHERE user_id = #{userId}
              AND deleted_at IS NULL
            ORDER BY snapshot_time DESC, id DESC
            LIMIT 1
            """)
    BtgMt5AccountSnapshot selectLatestByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT *
            FROM btg_mt5_account_snapshot
            WHERE account_id = #{accountId}
              AND deleted_at IS NULL
            ORDER BY snapshot_time DESC, id DESC
            LIMIT 1
            """)
    BtgMt5AccountSnapshot selectLatestByAccountId(@Param("accountId") String accountId);
}

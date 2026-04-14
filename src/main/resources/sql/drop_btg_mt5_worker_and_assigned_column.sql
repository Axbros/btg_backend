-- 下线 MT5 worker：删除 worker 表，并移除用户资料上的分配字段（按需执行）
DROP TABLE IF EXISTS btg_mt5_worker;

ALTER TABLE btg_user_profile
    DROP COLUMN assigned_worker_id;

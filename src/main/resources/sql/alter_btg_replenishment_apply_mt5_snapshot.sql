-- 补仓申请关联提交时的 MT5 快照；执行一次
ALTER TABLE btg_replenishment_apply
    ADD COLUMN mt5_snapshot_id BIGINT UNSIGNED NULL COMMENT '提交/重提交时申请人 btg_mt5_account_snapshot 最新一条 id' AFTER user_id;

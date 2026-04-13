-- MT5 账户快照（EA 上报）；执行一次
CREATE TABLE IF NOT EXISTS btg_mt5_account_snapshot
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT UNSIGNED          NULL COMMENT '关联 btg_user_profile.user_id；未匹配到则为 NULL',
    account_id     VARCHAR(100)     NOT NULL COMMENT 'MT5 账户号，对应 profile.trading_account_id',
    server_name    VARCHAR(255)     NOT NULL COMMENT 'MT5 服务器名',
    balance        DECIMAL(20, 8)   NOT NULL COMMENT '当前余额（上报 balance / now_balance）',
    equity         DECIMAL(20, 8)   NOT NULL COMMENT '当前净值',
    last_balance   DECIMAL(20, 8)   NOT NULL COMMENT '上次余额',
    last_equity    DECIMAL(20, 8)   NOT NULL COMMENT '上次净值',
    profit         DECIMAL(20, 8)            NULL COMMENT '盈亏（可选）',
    margin_amount  DECIMAL(20, 8)            NULL COMMENT '已用保证金（可选）',
    free_margin    DECIMAL(20, 8)            NULL COMMENT '可用保证金（可选）',
    margin_level   DECIMAL(20, 8)            NULL COMMENT '保证金比例/水平（可选）',
    source         VARCHAR(32)      NOT NULL DEFAULT 'EA_PUSH' COMMENT '数据来源',
    snapshot_time  DATETIME(3)      NOT NULL COMMENT 'EA 上报时间或后端接收时间',
    raw_payload    JSON                      NULL COMMENT '原始上报 JSON',
    created_at     DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at     DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted_at     DATETIME(3)               NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    KEY idx_account_id (account_id),
    KEY idx_user_id (user_id),
    KEY idx_snapshot_time (snapshot_time),
    KEY idx_account_snapshot_time (account_id, snapshot_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='MT5 账户快照';

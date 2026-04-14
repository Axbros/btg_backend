-- MT5 Worker 管理表；执行一次
-- btg_user_profile.assigned_worker_id 关联本表 id
CREATE TABLE IF NOT EXISTS btg_mt5_worker
(
    id                         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    worker_code                VARCHAR(100)    NOT NULL COMMENT '逻辑 Worker 编码，如 worker-1',
    worker_name                VARCHAR(100)             NULL COMMENT '展示名称',
    status                     TINYINT         NOT NULL DEFAULT 1 COMMENT '0 离线 1 在线 2 禁用',
    max_accounts               INT             NOT NULL DEFAULT 4 COMMENT '最大可分配账号数',
    current_accounts           INT             NOT NULL DEFAULT 0 COMMENT '当前分配数（建议与统计同步）',
    last_heartbeat_time        DATETIME(3)              NULL COMMENT '最后心跳时间',
    heartbeat_expire_seconds   INT             NOT NULL DEFAULT 60 COMMENT '心跳有效秒数',
    version                    VARCHAR(50)              NULL COMMENT 'Worker 版本',
    host_name                  VARCHAR(255)             NULL COMMENT '主机名',
    ip_address                 VARCHAR(100)             NULL COMMENT 'IP',
    remark                     VARCHAR(500)             NULL COMMENT '备注',
    created_at                 DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at                 DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted_at                 DATETIME(3)              NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_worker_code (worker_code),
    KEY idx_status (status),
    KEY idx_last_heartbeat_time (last_heartbeat_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='MT5 Worker';

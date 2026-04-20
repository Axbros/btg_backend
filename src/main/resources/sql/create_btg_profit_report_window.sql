-- 利润上报时间窗口：每个自然日（Asia/Shanghai）一行，记录当日开始/结束结算；执行一次
CREATE TABLE IF NOT EXISTS btg_profit_report_window
(
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    business_date       DATE            NOT NULL COMMENT '窗口自然日（Asia/Shanghai）',
    opened_at           DATETIME(3)              NULL COMMENT '根用户开始结算时间',
    closed_at           DATETIME(3)              NULL COMMENT '根用户结束结算时间',
    opened_by_user_id   BIGINT UNSIGNED          NULL,
    closed_by_user_id   BIGINT UNSIGNED          NULL,
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at          DATETIME(3)              NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_business_date (business_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='利润上报开放窗口（按日一行）';

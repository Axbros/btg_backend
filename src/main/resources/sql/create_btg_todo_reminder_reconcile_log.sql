CREATE TABLE `btg_todo_reminder_reconcile_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '被对账用户',
  `metric_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标键',
  `legacy_count` int(11) NOT NULL COMMENT '旧口径统计值',
  `reminder_count` int(11) NOT NULL COMMENT 'reminder口径统计值',
  `diff_count` int(11) NOT NULL COMMENT '差值 = reminder - legacy',
  `compared_at` datetime NOT NULL COMMENT '对账时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_compared_at` (`user_id`, `compared_at`),
  KEY `idx_metric_compared_at` (`metric_key`, `compared_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='dashboard pending-summary 对账日志';

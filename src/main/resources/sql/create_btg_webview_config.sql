-- WebView 网关单表配置（安卓 App 拉取；启动图由端上本地资源）
-- 执行前请备份

CREATE TABLE `btg_webview_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用 1是0否',
  `web_url` varchar(500) NOT NULL COMMENT 'WebView加载的H5地址',
  `inject_js` text DEFAULT NULL COMMENT '注入给网页执行的JS',
  `inject_css` text DEFAULT NULL COMMENT '注入给网页的CSS字符串',
  `show_splash` tinyint NOT NULL DEFAULT 0 COMMENT '是否显示启动屏 1是0否',
  `splash_duration_ms` int NOT NULL DEFAULT 1000 COMMENT '启动屏展示毫秒数',
  `version` bigint NOT NULL DEFAULT 1 COMMENT '配置版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='WebView网关配置';

INSERT INTO `btg_webview_config`
(`enabled`, `web_url`, `inject_js`, `inject_css`, `show_splash`, `splash_duration_ms`, `version`, `remark`)
VALUES
(1, 'https://h5.example.com', '', '', 1, 1000, 1, '默认WebView配置');

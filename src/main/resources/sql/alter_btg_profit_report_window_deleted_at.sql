-- 利润上报窗口表增加软删除字段；已建表环境执行一次
ALTER TABLE btg_profit_report_window
    ADD COLUMN deleted_at DATETIME(3) NULL COMMENT '软删除时间' AFTER updated_at;

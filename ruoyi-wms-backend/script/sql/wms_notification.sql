-- 消息通知表
CREATE TABLE IF NOT EXISTS `wms_notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` VARCHAR(200) NOT NULL COMMENT '消息标题',
  `content` TEXT COMMENT '消息内容',
  `notify_type` VARCHAR(50) NOT NULL COMMENT '消息类型：STOCK_WARNING/AGV_TASK/CHECK_REMIND/SYSTEM',
  `biz_id` BIGINT DEFAULT NULL COMMENT '关联业务ID',
  `biz_no` VARCHAR(100) DEFAULT NULL COMMENT '关联业务编号',
  `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0未读/1已读',
  `receive_by` VARCHAR(64) DEFAULT NULL COMMENT '接收人用户名（空表示全体）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_notify_type` (`notify_type`),
  INDEX `idx_read_status` (`read_status`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息通知表';

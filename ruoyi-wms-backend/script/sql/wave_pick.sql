-- 波次拣货主表
CREATE TABLE IF NOT EXISTS `wms_wave_pick` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `wave_no` VARCHAR(50) NOT NULL COMMENT '波次号',
  `warehouse_id` BIGINT DEFAULT NULL COMMENT '仓库ID',
  `order_count` INT DEFAULT 0 COMMENT '包含出库单数量',
  `total_quantity` DECIMAL(20,2) DEFAULT 0 COMMENT '商品总数量',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待拣货/1拣货中/2已完成/3已取消',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_wave_no` (`wave_no`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='波次拣货主表';

-- 波次拣货明细表
CREATE TABLE IF NOT EXISTS `wms_wave_pick_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `wave_id` BIGINT NOT NULL COMMENT '波次ID',
  `shipment_order_id` BIGINT NOT NULL COMMENT '出库单ID',
  `shipment_order_no` VARCHAR(50) DEFAULT NULL COMMENT '出库单号',
  `sku_id` BIGINT DEFAULT NULL COMMENT '规格ID',
  `source_location` VARCHAR(50) DEFAULT NULL COMMENT '源库位',
  `container_no` VARCHAR(50) DEFAULT NULL COMMENT '容器号',
  `quantity` DECIMAL(20,2) DEFAULT NULL COMMENT '拣货数量',
  `pick_status` TINYINT NOT NULL DEFAULT 0 COMMENT '拣货状态：0待拣/1已拣',
  PRIMARY KEY (`id`),
  INDEX `idx_wave_id` (`wave_id`),
  INDEX `idx_shipment_order_id` (`shipment_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='波次拣货明细表';

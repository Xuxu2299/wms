-- ----------------------------
-- 库存快照表：记录每日库存快照
-- 适用表：wms_inventory_snapshot
-- ----------------------------

DROP TABLE IF EXISTS `wms_inventory_snapshot`;
CREATE TABLE `wms_inventory_snapshot`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sku_id` bigint(20) NULL DEFAULT NULL COMMENT '规格ID',
  `warehouse_id` bigint(20) NULL DEFAULT NULL COMMENT '所属仓库',
  `quantity` decimal(20, 2) NULL DEFAULT NULL COMMENT '库存',
  `min_stock` decimal(20, 2) NULL DEFAULT NULL COMMENT '安全库存下限',
  `max_stock` decimal(20, 2) NULL DEFAULT NULL COMMENT '安全库存上限',
  `snapshot_date` date NULL DEFAULT NULL COMMENT '快照日期',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_snapshot_date`(`snapshot_date`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '库存快照表' ROW_FORMAT = Dynamic;

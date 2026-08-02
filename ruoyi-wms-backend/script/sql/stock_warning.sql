-- ----------------------------
-- 库存预警系统：为商品规格增加安全库存上下限字段
-- 适用表：wms_item_sku
-- ----------------------------

ALTER TABLE `wms_item_sku` ADD COLUMN `min_stock` decimal(20,2) DEFAULT 0 COMMENT '安全库存下限';
ALTER TABLE `wms_item_sku` ADD COLUMN `max_stock` decimal(20,2) DEFAULT 0 COMMENT '安全库存上限';

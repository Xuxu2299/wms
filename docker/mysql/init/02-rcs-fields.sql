-- RCS 对接新增字段：容器号、源库位号、目标库位号
-- 首次通过 docker compose 初始化数据库时自动执行。

ALTER TABLE wms_receipt_order_detail
  ADD COLUMN IF NOT EXISTS container_no VARCHAR(100) DEFAULT NULL COMMENT '容器号（RCS对接）' AFTER amount,
  ADD COLUMN IF NOT EXISTS source_location VARCHAR(100) DEFAULT NULL COMMENT '源库位号（RCS node_id）' AFTER container_no,
  ADD COLUMN IF NOT EXISTS target_location VARCHAR(100) DEFAULT NULL COMMENT '目标库位号（RCS node_id）' AFTER source_location;

ALTER TABLE wms_shipment_order_detail
  ADD COLUMN IF NOT EXISTS container_no VARCHAR(100) DEFAULT NULL COMMENT '容器号（RCS对接）' AFTER amount,
  ADD COLUMN IF NOT EXISTS source_location VARCHAR(100) DEFAULT NULL COMMENT '源库位号（RCS node_id）' AFTER container_no,
  ADD COLUMN IF NOT EXISTS target_location VARCHAR(100) DEFAULT NULL COMMENT '目标库位号（RCS node_id）' AFTER source_location;

ALTER TABLE wms_movement_order_detail
  ADD COLUMN IF NOT EXISTS container_no VARCHAR(100) DEFAULT NULL COMMENT '容器号（RCS对接）' AFTER amount,
  ADD COLUMN IF NOT EXISTS source_location VARCHAR(100) DEFAULT NULL COMMENT '源库位号（RCS node_id）' AFTER container_no,
  ADD COLUMN IF NOT EXISTS target_location VARCHAR(100) DEFAULT NULL COMMENT '目标库位号（RCS node_id）' AFTER source_location;

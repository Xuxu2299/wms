-- ----------------------------
-- 存储时长自动移库规则
-- 当某规格在源仓库的入库时长超过阈值天数时，定时任务自动生成移库单
-- 将其从源仓库移至目标仓库。
-- ----------------------------

CREATE TABLE IF NOT EXISTS `wms_storage_duration_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sku_id` bigint(20) NULL COMMENT '规格ID（为空表示匹配所有规格）',
  `threshold_days` int(11) NOT NULL COMMENT '存储时长阈值（天）',
  `from_warehouse_id` bigint(20) NOT NULL COMMENT '源仓库ID',
  `to_warehouse_id` bigint(20) NOT NULL COMMENT '目标仓库ID',
  `enable_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '启用状态：0停用 1启用',
  `remark` varchar(255) NULL COMMENT '备注',
  `create_by` varchar(64) NULL COMMENT '创建者',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_by` varchar(64) NULL COMMENT '更新者',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_enable_status` (`enable_status`),
  INDEX `idx_sku_id` (`sku_id`),
  INDEX `idx_from_warehouse` (`from_warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储时长自动移库规则';

-- ----------------------------
-- 菜单注册（父菜单：基础资料 ID=1808758090157985794）
-- 幂等：先删后插
-- ----------------------------

-- 页面菜单
DELETE FROM `sys_menu` WHERE `menu_id` = 1830000000000000020;
INSERT INTO `sys_menu` VALUES (1830000000000000020, '存储时长移库', 1808758090157985794, 6, 'storageDurationRule', 'wms/basic/storageDurationRule/index', '', 1, 0, 'C', '1', '1', 'wms:storageDurationRule:list', 'time', 'admin', sysdate(), '', NULL, '存储时长自动移库规则菜单');

-- 按钮权限：查询
DELETE FROM `sys_menu` WHERE `menu_id` = 1830000000000000021;
INSERT INTO `sys_menu` VALUES (1830000000000000021, '规则查询', 1830000000000000020, 1, '', NULL, NULL, 1, 0, 'F', '1', '1', 'wms:storageDurationRule:list', '#', 'admin', sysdate(), '', NULL, '');

-- 按钮权限：编辑
DELETE FROM `sys_menu` WHERE `menu_id` = 1830000000000000022;
INSERT INTO `sys_menu` VALUES (1830000000000000022, '规则编辑', 1830000000000000020, 2, '', NULL, NULL, 1, 0, 'F', '1', '1', 'wms:storageDurationRule:edit', '#', 'admin', sysdate(), '', NULL, '');

-- 注意：超级管理员(admin角色)自动拥有所有菜单权限，无需在 sys_role_menu 中分配；
-- 其他角色需在「角色管理」中勾选本菜单后方可使用。

-- 数据库初始化菜单（仅管理员可见）
-- 菜单ID: 2200，父菜单: 系统管理(ID=1)

-- 删除已存在的记录（幂等操作）
DELETE FROM `sys_menu` WHERE `menu_id` = 2200;

-- 插入菜单
INSERT INTO `sys_menu` VALUES (2200, '数据库初始化', 1, 20, 'database', 'system/database/index', '', 0, 0, 'C', '1', '1', 'system:database:init', 'tool', 'admin', sysdate(), '', NULL, '数据库初始化菜单（仅管理员）');

-- 注意：超级管理员(admin角色)自动拥有所有菜单权限，无需在sys_role_menu中分配
-- 其他角色不可见此菜单（未分配sys_role_menu记录）

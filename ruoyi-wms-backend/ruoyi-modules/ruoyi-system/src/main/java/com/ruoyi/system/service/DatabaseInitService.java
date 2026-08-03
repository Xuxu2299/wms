package com.ruoyi.system.service;

import com.ruoyi.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据库初始化服务
 * 执行 SQL 脚本重置数据库为初始状态
 *
 * @author wms
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DatabaseInitService {

    private final DataSource dataSource;

    /**
     * SQL 脚本文件名（按执行顺序）
     */
    private static final String[] SQL_FILE_NAMES = {
        "wms.sql",
        "inventory_snapshot.sql",
        "stock_warning.sql",
        "wave_pick.sql",
        "wms_notification.sql",
        "database_menu.sql"
    };

    /**
     * SQL 脚本可能的文件系统路径前缀（按优先级尝试）
     * 1. script/sql/ — 后端工作目录为 ruoyi-wms-backend/ 时
     * 2. ruoyi-wms-backend/script/sql/ — 后端工作目录为项目根目录时
     * 3. ../script/sql/ — 后端工作目录为 ruoyi-wms-backend/xxx/ 时
     */
    private static final String[] SQL_PATH_PREFIXES = {
        "script/sql/",
        "ruoyi-wms-backend/script/sql/",
        "../script/sql/"
    };

    /**
     * 获取数据库状态信息
     */
    public Map<String, Object> getDatabaseStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            // 数据库基本信息
            status.put("databaseName", conn.getCatalog());
            status.put("databaseProduct", conn.getMetaData().getDatabaseProductName());
            status.put("databaseVersion", conn.getMetaData().getDatabaseProductVersion());

            // 统计表数量
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            status.put("tableCount", tables.size());
            status.put("tables", tables);

            // 统计 WMS 业务表数据量
            Map<String, Long> tableCounts = new LinkedHashMap<>();
            String[] wmsTables = {
                "wms_receipt_order", "wms_shipment_order", "wms_movement_order",
                "wms_check_order", "wms_inventory", "wms_inventory_history",
                "wms_item", "wms_item_sku", "wms_warehouse", "wms_location",
                "sys_user", "sys_role", "sys_menu"
            };
            for (String table : wmsTables) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM `" + table + "`")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            tableCounts.put(table, rs.getLong(1));
                        }
                    }
                } catch (Exception e) {
                    // 表可能不存在
                    tableCounts.put(table, -1L);
                }
            }
            status.put("tableDataCounts", tableCounts);

        } catch (Exception e) {
            log.error("获取数据库状态失败", e);
            throw new ServiceException("获取数据库状态失败: " + e.getMessage());
        }
        return status;
    }

    /**
     * 初始化数据库
     * 按顺序执行 SQL 脚本，重置为初始状态
     */
    public Map<String, Object> initializeDatabase() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> executedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalStatements = 0;

        try (Connection conn = dataSource.getConnection()) {
            // 关闭外键检查，避免 DROP TABLE 顺序问题
            try (PreparedStatement ps = conn.prepareStatement("SET FOREIGN_KEY_CHECKS = 0")) {
                ps.execute();
            }

            // 按顺序执行每个 SQL 文件
            for (String sqlFileName : SQL_FILE_NAMES) {
                try {
                    log.info("开始执行 SQL 脚本: {}", sqlFileName);
                    int stmtCount = executeSqlFile(conn, sqlFileName);
                    executedFiles.add(sqlFileName);
                    totalStatements += stmtCount;
                    log.info("SQL 脚本执行完成: {}, 执行了 {} 条语句", sqlFileName, stmtCount);
                } catch (Exception e) {
                    log.error("SQL 脚本执行失败: {}", sqlFileName, e);
                    errors.add(sqlFileName + ": " + e.getMessage());
                    // 某些增量脚本如果表已存在可能报错，继续执行下一个
                }
            }

            // 重新开启外键检查
            try (PreparedStatement ps = conn.prepareStatement("SET FOREIGN_KEY_CHECKS = 1")) {
                ps.execute();
            }

        } catch (Exception e) {
            log.error("数据库初始化失败", e);
            throw new ServiceException("数据库初始化失败: " + e.getMessage());
        }

        result.put("executedFiles", executedFiles);
        result.put("errors", errors);
        result.put("totalStatements", totalStatements);
        result.put("success", errors.isEmpty() && totalStatements > 0);
        result.put("message", errors.isEmpty() && totalStatements > 0
            ? "数据库初始化成功，共执行 " + totalStatements + " 条SQL语句，已重置为初始状态"
            : (totalStatements == 0
                ? "数据库初始化失败：未执行任何SQL语句，请检查SQL脚本文件是否存在"
                : "数据库初始化完成，但部分脚本执行有错误"));

        return result;
    }

    /**
     * 执行单个 SQL 文件
     * 依次尝试多个可能的文件系统路径，最后尝试 classpath
     * 将文件内容按分号分割为多条 SQL 语句逐条执行
     *
     * @param conn      数据库连接
     * @param sqlFileName SQL 文件名（如 wms.sql）
     * @return 实际执行的 SQL 语句数
     */
    private int executeSqlFile(Connection conn, String sqlFileName) throws Exception {
        String content = null;
        String resolvedPath = null;

        // 1. 尝试多个文件系统路径前缀
        for (String prefix : SQL_PATH_PREFIXES) {
            String candidatePath = prefix + sqlFileName;
            File file = new File(candidatePath);
            if (file.exists()) {
                resolvedPath = file.getAbsolutePath();
                log.info("从文件系统读取 SQL: {}", resolvedPath);
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                    content = reader.lines().collect(Collectors.joining("\n"));
                }
                break;
            }
        }

        // 2. 文件系统未找到，尝试从 classpath 读取
        if (content == null) {
            String classpathWithDir = "sql/" + sqlFileName;
            Resource resource = new ClassPathResource(classpathWithDir);
            if (!resource.exists()) {
                resource = new ClassPathResource(sqlFileName);
            }
            if (resource.exists()) {
                resolvedPath = "classpath:" + resource.getFilename();
                log.info("从 classpath 读取 SQL: {}", resolvedPath);
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    content = reader.lines().collect(Collectors.joining("\n"));
                }
            }
        }

        // 3. 所有路径都未找到，抛出异常（不再静默跳过）
        if (content == null) {
            String triedPaths = String.join(", ", SQL_PATH_PREFIXES) + sqlFileName + ", classpath:sql/" + sqlFileName;
            throw new RuntimeException("SQL 文件未找到: " + sqlFileName + " (尝试路径: " + triedPaths + ")");
        }

        // 按分号分割 SQL 语句
        List<String> statements = splitSqlStatements(content);
        int executedCount = 0;

        for (String sql : statements) {
            String trimmed = sql.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("/*")) {
                continue;
            }
            try (PreparedStatement ps = conn.prepareStatement(trimmed)) {
                ps.execute();
                executedCount++;
            } catch (Exception e) {
                // 忽略 "表已存在" 等非致命错误
                String msg = e.getMessage();
                if (msg != null && (msg.contains("already exists") || msg.contains("Duplicate"))) {
                    log.debug("忽略非致命错误: {}", msg);
                } else {
                    throw e;
                }
            }
        }

        return executedCount;
    }

    /**
     * 将 SQL 文件内容分割为多条语句
     * 处理注释和分号分割
     */
    private List<String> splitSqlStatements(String content) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inComment = false;
        boolean inLineComment = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            char next = (i + 1 < content.length()) ? content.charAt(i + 1) : '\0';

            // 处理行注释 --
            if (!inSingleQuote && !inDoubleQuote && !inComment && c == '-' && next == '-') {
                inLineComment = true;
            }
            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                }
                continue;
            }

            // 处理块注释 /* */
            if (!inSingleQuote && !inDoubleQuote && !inLineComment && c == '/' && next == '*') {
                inComment = true;
                i++; // 跳过 *
                continue;
            }
            if (inComment) {
                if (c == '*' && next == '/') {
                    inComment = false;
                    i++; // 跳过 /
                }
                continue;
            }

            // 处理引号
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }

            // 分号分割
            if (c == ';' && !inSingleQuote && !inDoubleQuote && !inComment && !inLineComment) {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // 最后一条语句（可能没有分号结尾）
        String last = current.toString().trim();
        if (!last.isEmpty() && !last.startsWith("--")) {
            statements.add(last);
        }

        return statements;
    }
}

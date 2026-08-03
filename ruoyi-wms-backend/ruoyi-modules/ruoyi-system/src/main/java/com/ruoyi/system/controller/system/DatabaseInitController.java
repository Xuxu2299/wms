package com.ruoyi.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.ratelimiter.annotation.RateLimiter;
import com.ruoyi.common.ratelimiter.enums.LimitType;
import com.ruoyi.common.redis.utils.RedisUtils;
import com.ruoyi.common.satoken.utils.LoginHelper;
import com.ruoyi.common.web.core.BaseController;
import com.ruoyi.system.service.DatabaseInitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据库初始化管理
 * 仅超级管理员可访问
 *
 * @author wms
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/database")
public class DatabaseInitController extends BaseController {

    private final DatabaseInitService databaseInitService;

    /**
     * 获取数据库状态信息
     */
    @SaCheckRole("admin")
    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        return R.ok(databaseInitService.getDatabaseStatus());
    }

    /**
     * 初始化数据库（重置为初始状态）
     * 需传入 confirm="确认初始化" 进行二次确认
     */
    @SaCheckRole("admin")
    @Log(title = "数据库初始化", businessType = BusinessType.OTHER)
    @RateLimiter(time = 3600, count = 3, limitType = LimitType.DEFAULT)
    @PostMapping("/init")
    public R<Map<String, Object>> init(@RequestBody Map<String, String> body) {
        String confirm = body.get("confirm");
        if (!"确认初始化".equals(confirm)) {
            return R.fail("请输入正确的确认文字");
        }
        log.warn("管理员 {} 正在执行数据库初始化操作", LoginHelper.getUsername());
        Map<String, Object> result = databaseInitService.initializeDatabase();
        // 清除 Redis 缓存
        RedisUtils.deleteKeys("*");
        return R.ok(result);
    }
}

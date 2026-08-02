package com.ruoyi.wms.handler;

import com.ruoyi.common.core.domain.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

/**
 * WMS 全局异常处理器
 * <p>
 * 统一处理数据库及数据访问相关异常，避免向前端暴露 SQL 等敏感细节。
 * 采用最高优先级，确保在通用异常处理器之前优先拦截数据相关异常；
 * 其它异常（运行时异常、业务异常、校验异常等）仍由通用处理器兜底处理。
 *
 * @author ruoyi-wms
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 数据库 SQL 异常，不向调用方暴露具体 SQL 细节
     */
    @ExceptionHandler(SQLException.class)
    public R<Void> handleSQLException(SQLException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}', 发生数据库异常", requestURI, e);
        return R.fail("数据库异常，请联系管理员");
    }

    /**
     * 主键或唯一索引冲突
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public R<Void> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}', 数据库中已存在记录", requestURI, e);
        return R.fail("数据库中已存在该记录，请联系管理员确认");
    }

    /**
     * 数据完整性异常（外键约束、字段超长等）
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public R<Void> handleDataIntegrityViolationException(DataIntegrityViolationException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}', 发生数据完整性异常", requestURI, e);
        return R.fail("数据完整性异常，请检查数据是否合规");
    }

    /**
     * 数据访问异常（MyBatis 等数据访问层异常统一兜底）
     */
    @ExceptionHandler(DataAccessException.class)
    public R<Void> handleDataAccessException(DataAccessException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}', 发生数据操作异常", requestURI, e);
        return R.fail("数据操作异常，请联系管理员");
    }

}

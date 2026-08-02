package com.ruoyi.wms.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.excel.utils.ExcelUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.mybatis.core.page.PageQuery;
import com.ruoyi.common.mybatis.core.page.TableDataInfo;
import com.ruoyi.common.web.core.BaseController;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.wms.domain.bo.AgvLogBo;
import com.ruoyi.wms.domain.vo.AgvLogVo;
import com.ruoyi.wms.service.AgvLogService;

import java.util.List;

/**
 * AGV 回调日志
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wms/agvLog")
public class AgvLogController extends BaseController {

    private final AgvLogService agvLogService;

    /**
     * 查询 AGV 日志列表
     */
    @SaCheckPermission("wms:agvLog:all")
    @GetMapping("/list")
    public TableDataInfo<AgvLogVo> list(AgvLogBo bo, PageQuery pageQuery) {
        return agvLogService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出 AGV 日志列表
     */
    @SaCheckPermission("wms:agvLog:all")
    @Log(title = "AGV日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(AgvLogBo bo, HttpServletResponse response) {
        List<AgvLogVo> list = agvLogService.queryList(bo);
        ExcelUtil.exportExcel(list, "AGV日志", AgvLogVo.class, response);
    }

    /**
     * 获取 AGV 日志详细信息
     */
    @SaCheckPermission("wms:agvLog:all")
    @GetMapping("/{id}")
    public R<AgvLogVo> getInfo(@NotNull(message = "主键不能为空")
                               @PathVariable Long id) {
        return R.ok(agvLogService.queryById(id));
    }

    /**
     * 删除 AGV 日志
     */
    @SaCheckPermission("wms:agvLog:all")
    @Log(title = "AGV日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        agvLogService.deleteByIds(List.of(ids));
        return R.ok();
    }

    /**
     * 清空 AGV 日志
     */
    @SaCheckPermission("wms:agvLog:all")
    @Log(title = "AGV日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/clean")
    public R<Void> clean() {
        agvLogService.clean();
        return R.ok();
    }
}

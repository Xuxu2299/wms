package com.ruoyi.wms.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.web.core.BaseController;
import com.ruoyi.wms.domain.vo.DashboardSummaryVo;
import com.ruoyi.wms.domain.vo.DashboardTrendVo;
import com.ruoyi.wms.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据大屏接口
 *
 * @author wms
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wms/dashboard")
public class DashboardController extends BaseController {

    private final DashboardService dashboardService;

    /**
     * 查询近7天出入库趋势
     */
    @GetMapping("/trend")
    public R<DashboardTrendVo> trend() {
        return R.ok(dashboardService.getTrend());
    }

    /**
     * 查询库位利用率与今日出入库汇总
     */
    @GetMapping("/summary")
    public R<DashboardSummaryVo> summary() {
        return R.ok(dashboardService.getSummary());
    }
}

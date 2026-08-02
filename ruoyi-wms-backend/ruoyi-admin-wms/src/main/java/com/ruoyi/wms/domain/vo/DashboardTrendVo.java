package com.ruoyi.wms.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数据大屏 - 近7天出入库趋势
 *
 * @author wms
 */
@Data
public class DashboardTrendVo {

    /**
     * 日期列表（MM-dd 格式）
     */
    private List<String> dates;

    /**
     * 入库数量列表
     */
    private List<BigDecimal> inboundQuantities;

    /**
     * 出库数量列表
     */
    private List<BigDecimal> outboundQuantities;
}

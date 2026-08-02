package com.ruoyi.wms.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 数据大屏 - 库位利用率与今日出入库汇总
 *
 * @author wms
 */
@Data
public class DashboardSummaryVo {

    /**
     * 库位总数
     */
    private Integer totalLocations;

    /**
     * 已占用库位数
     */
    private Integer occupiedLocations;

    /**
     * 今日入库单数
     */
    private Integer todayInboundCount;

    /**
     * 今日出库单数
     */
    private Integer todayOutboundCount;

    /**
     * 今日入库总数量
     */
    private BigDecimal todayInboundQuantity;

    /**
     * 今日出库总数量
     */
    private BigDecimal todayOutboundQuantity;
}

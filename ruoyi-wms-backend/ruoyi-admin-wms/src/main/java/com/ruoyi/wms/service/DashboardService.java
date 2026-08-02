package com.ruoyi.wms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.wms.domain.entity.Location;
import com.ruoyi.wms.domain.entity.ReceiptOrder;
import com.ruoyi.wms.domain.entity.ShipmentOrder;
import com.ruoyi.wms.domain.vo.DashboardSummaryVo;
import com.ruoyi.wms.domain.vo.DashboardTrendVo;
import com.ruoyi.wms.mapper.LocationMapper;
import com.ruoyi.wms.mapper.ReceiptOrderMapper;
import com.ruoyi.wms.mapper.ShipmentOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据大屏 Service
 *
 * @author wms
 */
@RequiredArgsConstructor
@Service
public class DashboardService {

    private final ReceiptOrderMapper receiptOrderMapper;
    private final ShipmentOrderMapper shipmentOrderMapper;
    private final LocationMapper locationMapper;

    /**
     * 查询近7天出入库趋势
     * <p>
     * 按天汇总入库单和出库单的总数量（排除作废订单 order_status = -1）。
     *
     * @return 趋势数据
     */
    public DashboardTrendVo getTrend() {
        DashboardTrendVo vo = new DashboardTrendVo();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> inboundQuantities = new ArrayList<>();
        List<BigDecimal> outboundQuantities = new ArrayList<>();

        LocalDate today = LocalDate.now();
        DateTimeFormatter shortFmt = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            // 入库：按 create_time 过滤当天，排除作废(-1)
            LambdaQueryWrapper<ReceiptOrder> inLqw = Wrappers.lambdaQuery();
            inLqw.ge(ReceiptOrder::getCreateTime, start);
            inLqw.lt(ReceiptOrder::getCreateTime, end);
            inLqw.ne(ReceiptOrder::getOrderStatus, -1);
            List<ReceiptOrder> inOrders = receiptOrderMapper.selectList(inLqw);
            BigDecimal inQty = inOrders.stream()
                .map(o -> o.getTotalQuantity() != null ? o.getTotalQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 出库
            LambdaQueryWrapper<ShipmentOrder> outLqw = Wrappers.lambdaQuery();
            outLqw.ge(ShipmentOrder::getCreateTime, start);
            outLqw.lt(ShipmentOrder::getCreateTime, end);
            outLqw.ne(ShipmentOrder::getOrderStatus, -1);
            List<ShipmentOrder> outOrders = shipmentOrderMapper.selectList(outLqw);
            BigDecimal outQty = outOrders.stream()
                .map(o -> o.getTotalQuantity() != null ? o.getTotalQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            dates.add(date.format(shortFmt));
            inboundQuantities.add(inQty);
            outboundQuantities.add(outQty);
        }

        vo.setDates(dates);
        vo.setInboundQuantities(inboundQuantities);
        vo.setOutboundQuantities(outboundQuantities);
        return vo;
    }

    /**
     * 查询库位利用率与今日出入库汇总
     *
     * @return 汇总数据
     */
    public DashboardSummaryVo getSummary() {
        DashboardSummaryVo vo = new DashboardSummaryVo();

        // 库位利用率：统计全部库位与已占用库位（status = 1）
        LambdaQueryWrapper<Location> allLqw = Wrappers.lambdaQuery();
        List<Location> allLocations = locationMapper.selectList(allLqw);
        vo.setTotalLocations(allLocations.size());
        int occupied = (int) allLocations.stream()
            .filter(l -> l.getStatus() != null && l.getStatus() == 1)
            .count();
        vo.setOccupiedLocations(occupied);

        // 今日出入库汇总
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        // 今日入库
        LambdaQueryWrapper<ReceiptOrder> inLqw = Wrappers.lambdaQuery();
        inLqw.ge(ReceiptOrder::getCreateTime, start);
        inLqw.lt(ReceiptOrder::getCreateTime, end);
        inLqw.ne(ReceiptOrder::getOrderStatus, -1);
        List<ReceiptOrder> inOrders = receiptOrderMapper.selectList(inLqw);
        vo.setTodayInboundCount(inOrders.size());
        vo.setTodayInboundQuantity(inOrders.stream()
            .map(o -> o.getTotalQuantity() != null ? o.getTotalQuantity() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 今日出库
        LambdaQueryWrapper<ShipmentOrder> outLqw = Wrappers.lambdaQuery();
        outLqw.ge(ShipmentOrder::getCreateTime, start);
        outLqw.lt(ShipmentOrder::getCreateTime, end);
        outLqw.ne(ShipmentOrder::getOrderStatus, -1);
        List<ShipmentOrder> outOrders = shipmentOrderMapper.selectList(outLqw);
        vo.setTodayOutboundCount(outOrders.size());
        vo.setTodayOutboundQuantity(outOrders.stream()
            .map(o -> o.getTotalQuantity() != null ? o.getTotalQuantity() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

        return vo;
    }
}

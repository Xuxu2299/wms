package com.ruoyi.wms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.wms.domain.entity.Location;
import com.ruoyi.wms.domain.entity.ReceiptOrderDetail;
import com.ruoyi.wms.domain.entity.ShipmentOrderDetail;
import com.ruoyi.wms.domain.vo.LocationInventoryVo;
import com.ruoyi.wms.domain.vo.LocationVo;
import com.ruoyi.wms.domain.vo.ReceiptOrderDetailVo;
import com.ruoyi.wms.domain.vo.ShipmentOrderDetailVo;
import com.ruoyi.wms.mapper.LocationMapper;
import com.ruoyi.wms.mapper.ReceiptOrderDetailMapper;
import com.ruoyi.wms.mapper.ShipmentOrderDetailMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 库位 Service
 */
@RequiredArgsConstructor
@Service
public class LocationService extends ServiceImpl<LocationMapper, Location> {

    private final LocationMapper locationMapper;
    private final ReceiptOrderDetailMapper receiptOrderDetailMapper;
    private final ShipmentOrderDetailMapper shipmentOrderDetailMapper;

    /**
     * 查询指定区域的库位列表
     */
    public List<LocationVo> listByArea(String area) {
        LambdaQueryWrapper<Location> lqw = Wrappers.lambdaQuery();
        lqw.eq(Location::getArea, area);
        lqw.orderByAsc(Location::getLocationCode);
        return locationMapper.selectVoList(lqw);
    }

    /**
     * 查询指定区域、指定状态的库位列表
     */
    public List<LocationVo> listByAreaAndStatus(String area, Integer status) {
        LambdaQueryWrapper<Location> lqw = Wrappers.lambdaQuery();
        lqw.eq(Location::getArea, area);
        lqw.eq(Location::getStatus, status);
        lqw.orderByAsc(Location::getLocationCode);
        return locationMapper.selectVoList(lqw);
    }

    /**
     * 查询 A 区空库位
     */
    public List<LocationVo> listEmptyStorage() {
        return listByAreaAndStatus("A", 0);
    }

    /**
     * 查询 A 区有货库位
     */
    public List<LocationVo> listOccupiedStorage() {
        return listByAreaAndStatus("A", 1);
    }

    /**
     * 查询入库起点库位
     */
    public List<LocationVo> listReceiptStart() {
        return listByArea("R");
    }

    /**
     * 查询出库终点库位
     */
    public List<LocationVo> listShipmentEnd() {
        return listByArea("C");
    }

    /**
     * 查询所有库位（管理页面用）
     */
    public List<LocationVo> listAll() {
        LambdaQueryWrapper<Location> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(Location::getArea);
        lqw.orderByAsc(Location::getLocationCode);
        return locationMapper.selectVoList(lqw);
    }

    /**
     * 新增库位。
     * <p>
     * 校验库位编码唯一性，新增库位默认状态为空位(0)。
     *
     * @param location 库位信息
     */
    public void saveLocation(Location location) {
        if (location.getLocationCode() == null || location.getLocationCode().trim().isEmpty()) {
            throw new com.ruoyi.common.core.exception.ServiceException("库位编码不能为空",
                com.ruoyi.common.core.constant.HttpStatus.BAD_REQUEST, "库位编码不能为空");
        }
        // 校验编码唯一性
        LambdaQueryWrapper<Location> checkLqw = Wrappers.lambdaQuery();
        checkLqw.eq(Location::getLocationCode, location.getLocationCode().trim());
        if (locationMapper.selectCount(checkLqw) > 0) {
            throw new com.ruoyi.common.core.exception.ServiceException("库位编码重复",
                com.ruoyi.common.core.constant.HttpStatus.CONFLICT,
                "库位编码「" + location.getLocationCode() + "」已存在");
        }
        // 新增库位默认为空位
        location.setStatus(0);
        location.setContainerNo(null);
        locationMapper.insert(location);
    }

    /**
     * 修改库位。
     * <p>
     * 可修改库位编码、区域、所属仓库、备注。
     * 不允许修改状态和容器号（通过释放操作管理）。
     * 如果修改了库位编码，校验新编码的唯一性。
     *
     * @param location 库位信息
     */
    public void updateLocation(Location location) {
        if (location.getId() == null) {
            throw new com.ruoyi.common.core.exception.ServiceException("库位ID不能为空",
                com.ruoyi.common.core.constant.HttpStatus.BAD_REQUEST, "库位ID不能为空");
        }
        Location existing = locationMapper.selectById(location.getId());
        if (existing == null) {
            throw new com.ruoyi.common.core.exception.ServiceException("库位不存在",
                com.ruoyi.common.core.constant.HttpStatus.NOT_FOUND, "库位不存在");
        }
        // 如果编码有变化，校验新编码唯一性
        if (location.getLocationCode() != null && !location.getLocationCode().equals(existing.getLocationCode())) {
            LambdaQueryWrapper<Location> checkLqw = Wrappers.lambdaQuery();
            checkLqw.eq(Location::getLocationCode, location.getLocationCode().trim());
            checkLqw.ne(Location::getId, location.getId());
            if (locationMapper.selectCount(checkLqw) > 0) {
                throw new com.ruoyi.common.core.exception.ServiceException("库位编码重复",
                    com.ruoyi.common.core.constant.HttpStatus.CONFLICT,
                    "库位编码「" + location.getLocationCode() + "」已存在");
            }
        }
        // 不修改 status 和 containerNo（通过释放操作管理）
        location.setStatus(null);
        location.setContainerNo(null);
        locationMapper.updateById(location);
    }

    /**
     * 删除库位。
     * <p>
     * 安全检查：有货(status=1)的库位不允许删除，需先释放。
     *
     * @param id 库位ID
     */
    public void deleteLocation(Long id) {
        Location existing = locationMapper.selectById(id);
        if (existing == null) {
            throw new com.ruoyi.common.core.exception.ServiceException("库位不存在",
                com.ruoyi.common.core.constant.HttpStatus.NOT_FOUND, "库位不存在");
        }
        if (existing.getStatus() != null && existing.getStatus() == 1) {
            throw new com.ruoyi.common.core.exception.ServiceException("删除失败",
                com.ruoyi.common.core.constant.HttpStatus.CONFLICT,
                "库位「" + existing.getLocationCode() + "」当前有货，请先释放后再删除");
        }
        locationMapper.deleteById(id);
    }

    /**
     * 释放库位（将库位重置为空位，清除容器号）
     *
     * @param locationCodes 库位编码列表
     * @return 释放的库位数量
     */
    public int releaseLocations(List<String> locationCodes) {
        if (locationCodes == null || locationCodes.isEmpty()) {
            return 0;
        }
        LambdaQueryWrapper<Location> lqw = Wrappers.lambdaQuery();
        lqw.in(Location::getLocationCode, locationCodes);
        Location update = new Location();
        update.setStatus(0);
        update.setContainerNo(null);
        return locationMapper.update(update, lqw);
    }

    /**
     * 根据库位编码更新状态
     */
    public void updateStatusByCode(String locationCode, Integer status) {
        LambdaQueryWrapper<Location> lqw = Wrappers.lambdaQuery();
        lqw.eq(Location::getLocationCode, locationCode);
        Location update = new Location();
        update.setStatus(status);
        locationMapper.update(update, lqw);
    }

    /**
     * 根据库位编码更新状态和容器号。
     * <p>
     * 入库时：status=1，containerNo=入库容器号
     * 出库时：status=0，containerNo=null
     *
     * @param locationCode 库位编码
     * @param status       状态（0=空，1=有货）
     * @param containerNo  容器号（清空时传 null）
     */
    public void updateStatusAndContainerByCode(String locationCode, Integer status, String containerNo) {
        LambdaQueryWrapper<Location> lqw = Wrappers.lambdaQuery();
        lqw.eq(Location::getLocationCode, locationCode);
        Location update = new Location();
        update.setStatus(status);
        update.setContainerNo(containerNo);
        locationMapper.update(update, lqw);
    }

    /**
     * 根据库位编码查询容器号。
     * <p>
     * 优先从 wms_location 表查询，如果为空则从入库明细表
     * 中查找该库位最近一次入库时使用的容器号（兜底逻辑，
     * 兼容旧数据——旧入库操作未更新库位表 container_no）。
     *
     * @param locationCode 库位编码
     * @return 容器号，无则返回 null
     */
    public String getContainerNoByCode(String locationCode) {
        LambdaQueryWrapper<Location> lqw = Wrappers.lambdaQuery();
        lqw.eq(Location::getLocationCode, locationCode);
        Location location = locationMapper.selectOne(lqw);
        if (location != null && location.getContainerNo() != null && !location.getContainerNo().isEmpty()) {
            return location.getContainerNo();
        }
        // 兜底：从入库明细表查找该库位最近一次入库的容器号
        return locationMapper.selectContainerNoFromReceiptDetail(locationCode);
    }

    /**
     * 生成唯一的容器号。
     * <p>
     * 格式：从 00001 开始递增，每次加1，5位零填充。
     * 通过查询所有表中的最大容器号来确保唯一性。
     *
     * @return 唯一容器号（如 00001、00002）
     */
    public synchronized String generateContainerNo() {
        String maxNo = locationMapper.selectMaxContainerNo();
        int next = 1;
        if (maxNo != null) {
            try {
                next = Integer.parseInt(maxNo) + 1;
            } catch (NumberFormatException e) {
                // 解析失败则从1开始
                next = 1;
            }
        }
        return String.format("%05d", next);
    }

    /**
     * 查询指定SKU在各库位的剩余库存（出库自动拆分用）。
     * <p>
     * 计算方式：入库明细（按targetLocation汇总）- 出库明细（按sourceLocation汇总），
     * 只返回剩余数量大于0的库位，按库位编码自然排序（A1 < A2 < A10）。
     * 仅统计已处理订单（order_status IN (1,2)），排除暂存(0)和作废(-1)的订单。
     *
     * @param skuId 规格ID
     * @return 库位库存列表
     */
    public List<LocationInventoryVo> listInventoryBySku(Long skuId) {
        if (skuId == null) {
            return Collections.emptyList();
        }

        // 1. 查询该SKU所有已处理入库单的明细（order_status IN (1,2)），按targetLocation汇总
        LambdaQueryWrapper<ReceiptOrderDetail> inLqw = Wrappers.lambdaQuery();
        inLqw.eq(ReceiptOrderDetail::getSkuId, skuId);
        inLqw.isNotNull(ReceiptOrderDetail::getTargetLocation);
        inLqw.ne(ReceiptOrderDetail::getTargetLocation, "");
        inLqw.inSql(ReceiptOrderDetail::getOrderId,
            "SELECT id FROM wms_receipt_order WHERE order_status IN (1, 2)");
        List<ReceiptOrderDetailVo> inDetails = receiptOrderDetailMapper.selectVoList(inLqw);

        Map<String, BigDecimal> inMap = new HashMap<>();
        Map<String, String> containerMap = new HashMap<>();
        for (ReceiptOrderDetailVo d : inDetails) {
            String loc = d.getTargetLocation();
            BigDecimal qty = d.getQuantity() != null ? d.getQuantity() : BigDecimal.ZERO;
            inMap.merge(loc, qty, BigDecimal::add);
            if (d.getContainerNo() != null && !d.getContainerNo().isEmpty()) {
                containerMap.putIfAbsent(loc, d.getContainerNo());
            }
        }

        // 2. 查询该SKU所有已处理出库单的明细（order_status IN (1,2)），按sourceLocation汇总
        LambdaQueryWrapper<ShipmentOrderDetail> outLqw = Wrappers.lambdaQuery();
        outLqw.eq(ShipmentOrderDetail::getSkuId, skuId);
        outLqw.isNotNull(ShipmentOrderDetail::getSourceLocation);
        outLqw.ne(ShipmentOrderDetail::getSourceLocation, "");
        outLqw.inSql(ShipmentOrderDetail::getOrderId,
            "SELECT id FROM wms_shipment_order WHERE order_status IN (1, 2)");
        List<ShipmentOrderDetailVo> outDetails = shipmentOrderDetailMapper.selectVoList(outLqw);

        Map<String, BigDecimal> outMap = new HashMap<>();
        for (ShipmentOrderDetailVo d : outDetails) {
            String loc = d.getSourceLocation();
            BigDecimal qty = d.getQuantity() != null ? d.getQuantity() : BigDecimal.ZERO;
            outMap.merge(loc, qty, BigDecimal::add);
        }

        // 3. 合并计算各库位当前库存
        Set<String> allLocations = new HashSet<>(inMap.keySet());
        allLocations.addAll(outMap.keySet());

        List<LocationInventoryVo> result = new ArrayList<>();
        for (String loc : allLocations) {
            BigDecimal inQty = inMap.getOrDefault(loc, BigDecimal.ZERO);
            BigDecimal outQty = outMap.getOrDefault(loc, BigDecimal.ZERO);
            BigDecimal remaining = inQty.subtract(outQty);
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                LocationInventoryVo vo = new LocationInventoryVo();
                vo.setLocationCode(loc);
                vo.setQuantity(remaining);
                vo.setContainerNo(containerMap.get(loc));
                result.add(vo);
            }
        }

        // 4. 按库位编码自然排序（A1 < A2 < A10）
        result.sort(Comparator.comparingInt(vo -> {
            String numStr = vo.getLocationCode().replaceAll("[^0-9]", "");
            return numStr.isEmpty() ? 0 : Integer.parseInt(numStr);
        }));

        return result;
    }

    /**
     * FEFO推荐出库库位：按效期就近原则推荐出库库位。
     * <p>
     * 在 listInventoryBySku 的基础上，查询各库位对应入库明细中最早的过期日期，
     * 按过期日期升序排序（最早过期优先出库 = FEFO）。
     * 仅统计已处理订单（order_status IN (1,2)），排除暂存(0)和作废(-1)的订单。
     * 无过期日期记录的库位排在最后。
     *
     * @param skuId 规格ID
     * @return 按效期升序排序的库位库存列表
     */
    public List<LocationInventoryVo> listFifoRecommend(Long skuId) {
        // 1. 获取该SKU在各库位的剩余库存（已按库位编码自然排序）
        List<LocationInventoryVo> inventoryList = listInventoryBySku(skuId);
        if (inventoryList.isEmpty()) {
            return inventoryList;
        }

        // 2. 查询该SKU所有已处理入库单的明细（order_status IN (1,2)），获取各库位最早过期日期
        LambdaQueryWrapper<ReceiptOrderDetail> detailLqw = Wrappers.lambdaQuery();
        detailLqw.eq(ReceiptOrderDetail::getSkuId, skuId);
        detailLqw.isNotNull(ReceiptOrderDetail::getTargetLocation);
        detailLqw.ne(ReceiptOrderDetail::getTargetLocation, "");
        detailLqw.isNotNull(ReceiptOrderDetail::getExpiryDate);
        detailLqw.inSql(ReceiptOrderDetail::getOrderId,
            "SELECT id FROM wms_receipt_order WHERE order_status IN (1, 2)");
        List<ReceiptOrderDetailVo> details = receiptOrderDetailMapper.selectVoList(detailLqw);

        // 3. 按targetLocation分组，取每个库位最早的过期日期
        Map<String, LocalDate> earliestExpiryMap = new HashMap<>();
        for (ReceiptOrderDetailVo d : details) {
            String loc = d.getTargetLocation();
            LocalDate expiry = d.getExpiryDate();
            earliestExpiryMap.merge(loc, expiry,
                (existing, current) -> current.isBefore(existing) ? current : existing);
        }

        // 4. 按过期日期升序排序（最早过期优先 = FEFO），无过期日期的库位排在最后
        inventoryList.sort(Comparator.comparing(
            vo -> earliestExpiryMap.getOrDefault(vo.getLocationCode(), LocalDate.MAX)));

        return inventoryList;
    }
}

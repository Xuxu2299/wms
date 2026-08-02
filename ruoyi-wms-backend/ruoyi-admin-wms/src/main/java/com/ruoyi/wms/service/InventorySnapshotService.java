package com.ruoyi.wms.service;

import cn.hutool.core.collection.CollUtil;
import com.ruoyi.wms.domain.entity.Inventory;
import com.ruoyi.wms.domain.entity.InventorySnapshot;
import com.ruoyi.wms.domain.entity.Warehouse;
import com.ruoyi.wms.domain.vo.InventorySnapshotVo;
import com.ruoyi.wms.domain.vo.ItemSkuMapVo;
import com.ruoyi.wms.mapper.InventorySnapshotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 库存快照 Service业务层处理
 *
 * @author zcc
 * @date 2024-07-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventorySnapshotService {

    private final InventorySnapshotMapper inventorySnapshotMapper;
    private final InventoryService inventoryService;
    private final ItemSkuService itemSkuService;
    private final WarehouseService warehouseService;

    /**
     * 创建库存快照：查询当前所有库存，为今天创建快照记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void createSnapshot() {
        LocalDate today = LocalDate.now();
        // 检查今天是否已创建快照
        List<InventorySnapshot> existing = inventorySnapshotMapper.selectByDate(today);
        if (CollUtil.isNotEmpty(existing)) {
            log.info("库存快照已存在，日期：{}", today);
            return;
        }
        // 查询所有当前库存
        List<Inventory> inventoryList = inventoryService.list();
        if (CollUtil.isEmpty(inventoryList)) {
            log.info("无库存数据，跳过快照");
            return;
        }
        // 批量查询SKU信息以获取安全库存上下限
        Set<Long> skuIds = inventoryList.stream()
            .map(Inventory::getSkuId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, ItemSkuMapVo> itemSkuMap = CollUtil.isEmpty(skuIds)
            ? Collections.emptyMap()
            : itemSkuService.queryItemSkuMapVosByIds(skuIds);
        // 构建快照记录
        List<InventorySnapshot> snapshots = new ArrayList<>();
        for (Inventory inventory : inventoryList) {
            InventorySnapshot snapshot = new InventorySnapshot();
            snapshot.setSkuId(inventory.getSkuId());
            snapshot.setWarehouseId(inventory.getWarehouseId());
            snapshot.setQuantity(inventory.getQuantity());
            snapshot.setSnapshotDate(today);
            // 从SKU信息中获取安全库存上下限
            ItemSkuMapVo skuMapVo = itemSkuMap.get(inventory.getSkuId());
            if (skuMapVo != null && skuMapVo.getItemSku() != null) {
                snapshot.setMinStock(skuMapVo.getItemSku().getMinStock());
                snapshot.setMaxStock(skuMapVo.getItemSku().getMaxStock());
            }
            snapshots.add(snapshot);
        }
        // 批量插入
        inventorySnapshotMapper.insertBatch(snapshots);
        log.info("成功创建 {} 条库存快照记录，日期：{}", snapshots.size(), today);
    }

    /**
     * 按日期查询库存快照，包含SKU/商品名称和仓库名称
     *
     * @param date 快照日期
     * @return 快照视图对象列表
     */
    public List<InventorySnapshotVo> queryByDate(LocalDate date) {
        List<InventorySnapshot> snapshots = inventorySnapshotMapper.selectByDate(date);
        if (CollUtil.isEmpty(snapshots)) {
            return Collections.emptyList();
        }
        // 批量查询SKU信息
        Set<Long> skuIds = snapshots.stream()
            .map(InventorySnapshot::getSkuId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, ItemSkuMapVo> itemSkuMap = CollUtil.isEmpty(skuIds)
            ? Collections.emptyMap()
            : itemSkuService.queryItemSkuMapVosByIds(skuIds);
        // 批量查询仓库信息
        Set<Long> warehouseIds = snapshots.stream()
            .map(InventorySnapshot::getWarehouseId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, Warehouse> warehouseMap = CollUtil.isEmpty(warehouseIds)
            ? Collections.emptyMap()
            : warehouseService.listByIds(warehouseIds).stream()
                .collect(Collectors.toMap(Warehouse::getId, w -> w));
        // 构建VO列表
        List<InventorySnapshotVo> voList = new ArrayList<>();
        for (InventorySnapshot snapshot : snapshots) {
            InventorySnapshotVo vo = new InventorySnapshotVo();
            vo.setId(snapshot.getId());
            vo.setSkuId(snapshot.getSkuId());
            vo.setWarehouseId(snapshot.getWarehouseId());
            vo.setQuantity(snapshot.getQuantity());
            vo.setMinStock(snapshot.getMinStock());
            vo.setMaxStock(snapshot.getMaxStock());
            vo.setSnapshotDate(snapshot.getSnapshotDate());
            // 设置规格名称和商品名称
            ItemSkuMapVo skuMapVo = itemSkuMap.get(snapshot.getSkuId());
            if (skuMapVo != null) {
                if (skuMapVo.getItemSku() != null) {
                    vo.setSkuName(skuMapVo.getItemSku().getSkuName());
                }
                if (skuMapVo.getItem() != null) {
                    vo.setItemName(skuMapVo.getItem().getItemName());
                }
            }
            // 设置仓库名称
            Warehouse warehouse = warehouseMap.get(snapshot.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getWarehouseName());
            }
            voList.add(vo);
        }
        return voList;
    }

    /**
     * 查询可用的快照日期列表
     *
     * @return 快照日期列表
     */
    public List<LocalDate> listSnapshotDates() {
        return inventorySnapshotMapper.selectRecentDates();
    }

}

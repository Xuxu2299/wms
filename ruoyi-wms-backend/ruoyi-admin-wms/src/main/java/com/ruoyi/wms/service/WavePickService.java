package com.ruoyi.wms.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.wms.domain.entity.WavePick;
import com.ruoyi.wms.domain.entity.WavePickDetail;
import com.ruoyi.wms.domain.vo.ItemSkuMapVo;
import com.ruoyi.wms.domain.vo.ShipmentOrderDetailVo;
import com.ruoyi.wms.domain.vo.ShipmentOrderVo;
import com.ruoyi.wms.domain.vo.WavePickDetailVo;
import com.ruoyi.wms.domain.vo.WavePickVo;
import com.ruoyi.wms.mapper.WavePickDetailMapper;
import com.ruoyi.wms.mapper.WavePickMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 波次拣货 Service
 *
 * @author wms
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WavePickService {

    private final WavePickMapper wavePickMapper;
    private final WavePickDetailMapper wavePickDetailMapper;
    private final ShipmentOrderService shipmentOrderService;
    private final ItemSkuService itemSkuService;
    private final WarehouseService warehouseService;

    /**
     * 创建波次：将多个待出库单合并为一个波次
     *
     * @param shipmentOrderIds 出库单ID列表
     * @param warehouseId      仓库ID
     * @param remark           备注
     */
    @Transactional(rollbackFor = Exception.class)
    public WavePickVo createWave(List<Long> shipmentOrderIds, Long warehouseId, String remark) {
        if (CollUtil.isEmpty(shipmentOrderIds)) {
            throw new ServiceException("请选择出库单");
        }

        // 验证出库单状态
        BigDecimal totalQuantity = BigDecimal.ZERO;
        List<WavePickDetail> details = new ArrayList<>();
        for (Long orderId : shipmentOrderIds) {
            ShipmentOrderVo vo = shipmentOrderService.queryById(orderId);
            if (vo == null) {
                throw new ServiceException("出库单不存在：" + orderId);
            }
            if (vo.getOrderStatus() != null && vo.getOrderStatus() != 0) {
                throw new ServiceException("出库单 " + vo.getOrderNo() + " 状态不允许加入波次");
            }
            if (CollUtil.isNotEmpty(vo.getDetails())) {
                for (ShipmentOrderDetailVo detail : vo.getDetails()) {
                    WavePickDetail wpd = new WavePickDetail();
                    wpd.setShipmentOrderId(orderId);
                    wpd.setShipmentOrderNo(vo.getOrderNo());
                    wpd.setSkuId(detail.getSkuId());
                    wpd.setSourceLocation(detail.getSourceLocation());
                    wpd.setContainerNo(detail.getContainerNo());
                    wpd.setQuantity(detail.getQuantity());
                    wpd.setPickStatus(0);
                    details.add(wpd);
                    if (detail.getQuantity() != null) {
                        totalQuantity = totalQuantity.add(detail.getQuantity());
                    }
                }
            }
        }

        // 创建波次主记录
        WavePick wavePick = new WavePick();
        wavePick.setWaveNo("WAVE" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        wavePick.setWarehouseId(warehouseId);
        wavePick.setOrderCount(shipmentOrderIds.size());
        wavePick.setTotalQuantity(totalQuantity);
        wavePick.setStatus(0);
        wavePick.setRemark(remark);
        wavePick.setCreateTime(LocalDateTime.now());
        wavePickMapper.insert(wavePick);

        // 保存明细
        for (WavePickDetail detail : details) {
            detail.setWaveId(wavePick.getId());
            wavePickDetailMapper.insert(detail);
        }

        return queryById(wavePick.getId());
    }

    /**
     * 查询波次详情
     */
    public WavePickVo queryById(Long id) {
        WavePick wavePick = wavePickMapper.selectById(id);
        if (wavePick == null) {
            throw new ServiceException("波次不存在");
        }
        WavePickVo vo = new WavePickVo();
        vo.setId(wavePick.getId());
        vo.setWaveNo(wavePick.getWaveNo());
        vo.setWarehouseId(wavePick.getWarehouseId());
        vo.setOrderCount(wavePick.getOrderCount());
        vo.setTotalQuantity(wavePick.getTotalQuantity());
        vo.setStatus(wavePick.getStatus());
        vo.setRemark(wavePick.getRemark());
        vo.setCreateBy(wavePick.getCreateBy());
        vo.setCreateTime(wavePick.getCreateTime());

        // 设置仓库名称
        if (wavePick.getWarehouseId() != null) {
            warehouseService.listByIds(List.of(wavePick.getWarehouseId()))
                .stream().findFirst()
                .ifPresent(w -> vo.setWarehouseName(w.getWarehouseName()));
        }

        // 查询明细
        LambdaQueryWrapper<WavePickDetail> dlqw = new LambdaQueryWrapper<>();
        dlqw.eq(WavePickDetail::getWaveId, id);
        List<WavePickDetail> detailList = wavePickDetailMapper.selectList(dlqw);
        if (CollUtil.isNotEmpty(detailList)) {
            // 批量查询SKU信息
            Set<Long> skuIds = detailList.stream()
                .map(WavePickDetail::getSkuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            Map<Long, ItemSkuMapVo> skuMap = CollUtil.isEmpty(skuIds)
                ? Collections.emptyMap()
                : itemSkuService.queryItemSkuMapVosByIds(skuIds);

            List<WavePickDetailVo> detailVos = new ArrayList<>();
            for (WavePickDetail d : detailList) {
                WavePickDetailVo dvo = new WavePickDetailVo();
                dvo.setId(d.getId());
                dvo.setWaveId(d.getWaveId());
                dvo.setShipmentOrderId(d.getShipmentOrderId());
                dvo.setShipmentOrderNo(d.getShipmentOrderNo());
                dvo.setSkuId(d.getSkuId());
                dvo.setSourceLocation(d.getSourceLocation());
                dvo.setContainerNo(d.getContainerNo());
                dvo.setQuantity(d.getQuantity());
                dvo.setPickStatus(d.getPickStatus());
                ItemSkuMapVo skuMapVo = skuMap.get(d.getSkuId());
                if (skuMapVo != null) {
                    if (skuMapVo.getItemSku() != null) {
                        dvo.setSkuName(skuMapVo.getItemSku().getSkuName());
                    }
                    if (skuMapVo.getItem() != null) {
                        dvo.setItemName(skuMapVo.getItem().getItemName());
                    }
                }
                detailVos.add(dvo);
            }
            vo.setDetails(detailVos);
        }
        return vo;
    }

    /**
     * 分页查询波次列表
     */
    public IPage<WavePickVo> queryPage(Integer pageNum, Integer pageSize, Integer status) {
        LambdaQueryWrapper<WavePick> lqw = new LambdaQueryWrapper<>();
        if (status != null) {
            lqw.eq(WavePick::getStatus, status);
        }
        lqw.orderByDesc(WavePick::getCreateTime);
        IPage<WavePick> page = new Page<>(pageNum, pageSize);
        IPage<WavePick> result = wavePickMapper.selectPage(page, lqw);
        return result.convert(this::toVo);
    }

    /**
     * 标记明细已拣
     */
    @Transactional(rollbackFor = Exception.class)
    public void markPicked(Long detailId) {
        LambdaUpdateWrapper<WavePickDetail> uw = new LambdaUpdateWrapper<>();
        uw.eq(WavePickDetail::getId, detailId);
        uw.set(WavePickDetail::getPickStatus, 1);
        wavePickDetailMapper.update(null, uw);

        // 检查波次是否全部拣完
        WavePickDetail detail = wavePickDetailMapper.selectById(detailId);
        if (detail != null) {
            LambdaQueryWrapper<WavePickDetail> lqw = new LambdaQueryWrapper<>();
            lqw.eq(WavePickDetail::getWaveId, detail.getWaveId());
            lqw.eq(WavePickDetail::getPickStatus, 0);
            long unpicked = wavePickDetailMapper.selectCount(lqw);
            if (unpicked == 0) {
                // 全部拣完，更新波次状态
                LambdaUpdateWrapper<WavePick> wuw = new LambdaUpdateWrapper<>();
                wuw.eq(WavePick::getId, detail.getWaveId());
                wuw.set(WavePick::getStatus, 2);
                wuw.set(WavePick::getUpdateTime, LocalDateTime.now());
                wavePickMapper.update(null, wuw);
            } else {
                // 部分拣完，更新为拣货中
                LambdaUpdateWrapper<WavePick> wuw = new LambdaUpdateWrapper<>();
                wuw.eq(WavePick::getId, detail.getWaveId());
                wuw.eq(WavePick::getStatus, 0);
                wuw.set(WavePick::getStatus, 1);
                wuw.set(WavePick::getUpdateTime, LocalDateTime.now());
                wavePickMapper.update(null, wuw);
            }
        }
    }

    /**
     * 取消波次
     */
    public void cancel(Long id) {
        LambdaUpdateWrapper<WavePick> uw = new LambdaUpdateWrapper<>();
        uw.eq(WavePick::getId, id);
        uw.set(WavePick::getStatus, 3);
        uw.set(WavePick::getUpdateTime, LocalDateTime.now());
        wavePickMapper.update(null, uw);
    }

    /**
     * 删除波次
     */
    public void delete(Long id) {
        // 先删除明细
        LambdaQueryWrapper<WavePickDetail> dlqw = new LambdaQueryWrapper<>();
        dlqw.eq(WavePickDetail::getWaveId, id);
        wavePickDetailMapper.delete(dlqw);
        wavePickMapper.deleteById(id);
    }

    private WavePickVo toVo(WavePick entity) {
        WavePickVo vo = new WavePickVo();
        vo.setId(entity.getId());
        vo.setWaveNo(entity.getWaveNo());
        vo.setWarehouseId(entity.getWarehouseId());
        vo.setOrderCount(entity.getOrderCount());
        vo.setTotalQuantity(entity.getTotalQuantity());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

}

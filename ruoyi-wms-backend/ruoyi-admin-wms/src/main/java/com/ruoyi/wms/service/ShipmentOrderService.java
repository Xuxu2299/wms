package com.ruoyi.wms.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.constant.ServiceConstants;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.exception.base.BaseException;
import com.ruoyi.common.core.utils.MapstructUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.mybatis.core.domain.BaseEntity;
import com.ruoyi.common.mybatis.core.page.PageQuery;
import com.ruoyi.common.mybatis.core.page.TableDataInfo;
import com.ruoyi.wms.domain.bo.ShipmentOrderBo;
import com.ruoyi.wms.domain.bo.ShipmentOrderDetailBo;
import com.ruoyi.wms.domain.entity.ReceiptOrder;
import com.ruoyi.wms.domain.entity.ShipmentOrder;
import com.ruoyi.wms.domain.entity.ShipmentOrderDetail;
import com.ruoyi.wms.domain.vo.ReceiptOrderDetailVo;
import com.ruoyi.wms.domain.vo.ReceiptOrderVo;
import com.ruoyi.wms.domain.vo.ShipmentOrderDetailVo;
import com.ruoyi.wms.domain.vo.ShipmentOrderVo;
import com.ruoyi.wms.mapper.ShipmentOrderMapper;
import com.ruoyi.wms.rcs.RcsTaskHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 出库单Service业务层处理
 *
 * @author zcc
 * @date 2024-08-01
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ShipmentOrderService {

    private final ShipmentOrderMapper shipmentOrderMapper;
    private final ShipmentOrderDetailService shipmentOrderDetailService;
    private final InventoryService inventoryService;
    private final InventoryHistoryService inventoryHistoryService;
    private final RcsTaskHelper rcsTaskHelper;
    private final LocationService locationService;

    /**
     * 查询出库单
     */
    public ShipmentOrderVo queryById(Long id){
        ShipmentOrderVo shipmentOrderVo = shipmentOrderMapper.selectVoById(id);
        if (shipmentOrderVo == null) {
            throw new BaseException("出库单不存在");
        }
        shipmentOrderVo.setDetails(shipmentOrderDetailService.queryByShipmentOrderId(shipmentOrderVo.getId()));
        return shipmentOrderVo;
    }

    /**
     * 查询出库单列表
     */
    public TableDataInfo<ShipmentOrderVo> queryPageList(ShipmentOrderBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ShipmentOrder> lqw = buildQueryWrapper(bo);
        Page<ShipmentOrderVo> result = shipmentOrderMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询出库单列表
     */
    public List<ShipmentOrderVo> queryList(ShipmentOrderBo bo) {
        LambdaQueryWrapper<ShipmentOrder> lqw = buildQueryWrapper(bo);
        return shipmentOrderMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ShipmentOrder> buildQueryWrapper(ShipmentOrderBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ShipmentOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getOrderNo()), ShipmentOrder::getOrderNo, bo.getOrderNo());
        lqw.eq(bo.getOptType() != null, ShipmentOrder::getOptType, bo.getOptType());
        lqw.eq(StringUtils.isNotBlank(bo.getOrderNo()), ShipmentOrder::getOrderNo, bo.getOrderNo());
        lqw.eq(bo.getMerchantId() != null, ShipmentOrder::getMerchantId, bo.getMerchantId());
        lqw.eq(bo.getTotalAmount() != null, ShipmentOrder::getTotalAmount, bo.getTotalAmount());
        lqw.eq(bo.getTotalQuantity() != null, ShipmentOrder::getTotalQuantity, bo.getTotalQuantity());
        lqw.eq(bo.getOrderStatus() != null, ShipmentOrder::getOrderStatus, bo.getOrderStatus());
        lqw.orderByDesc(BaseEntity::getCreateTime);
        return lqw;
    }

    /**
     * 暂存出库单
     */
    @Transactional
    public Long insertByBo(ShipmentOrderBo bo) {
        // 校验出库单号唯一性
        validateShipmentOrderNo(bo.getOrderNo());
        // 创建出库单
        ShipmentOrder add = MapstructUtils.convert(bo, ShipmentOrder.class);
        shipmentOrderMapper.insert(add);
        bo.setId(add.getId());
        List<ShipmentOrderDetailBo> detailBoList = bo.getDetails();
        List<ShipmentOrderDetail> addDetailList = MapstructUtils.convert(detailBoList, ShipmentOrderDetail.class);
        addDetailList.forEach(it -> it.setOrderId(add.getId()));
        shipmentOrderDetailService.saveDetails(addDetailList);
        return add.getId();
    }

    public void validateShipmentOrderNo(String shipmentOrderNo) {
        LambdaQueryWrapper<ShipmentOrder> receiptOrderLqw = Wrappers.lambdaQuery();
        receiptOrderLqw.eq(ShipmentOrder::getOrderNo, shipmentOrderNo);
        ShipmentOrder shipmentOrder = shipmentOrderMapper.selectOne(receiptOrderLqw);
        Assert.isNull(shipmentOrder, "出库单号重复，请手动修改");
    }


    /**
     * 修改出库单
     */
    @Transactional
    public void updateByBo(ShipmentOrderBo bo) {
        // 更新出库单
        ShipmentOrder update = MapstructUtils.convert(bo, ShipmentOrder.class);
        shipmentOrderMapper.updateById(update);
        // 保存出库单明细
        List<ShipmentOrderDetail> detailList = MapstructUtils.convert(bo.getDetails(), ShipmentOrderDetail.class);

        //需要考虑detail删除
        List<ShipmentOrderDetailVo> dbList = shipmentOrderDetailService.queryByShipmentOrderId(bo.getId());
        Set<Long> ids = detailList.stream().filter(it -> it.getId() != null).map(it -> it.getId()).collect(Collectors.toSet());
        List<ShipmentOrderDetailVo> delList = dbList.stream().filter(it -> !ids.contains(it.getId())).collect(Collectors.toList());
        if (CollectionUtil.isNotEmpty(delList)) {
            shipmentOrderDetailService.deleteByIds(delList.stream().map(it->it.getId()).collect(Collectors.toList()));
        }
        detailList.forEach(it -> it.setOrderId(bo.getId()));
        shipmentOrderDetailService.saveDetails(detailList);
    }

    /**
     * 批量删除出库单
     */
    public void deleteById(Long id) {
        validateIdBeforeDelete(id);
        shipmentOrderMapper.deleteById(id);
    }

    public void validateIdBeforeDelete(Long id) {
        ShipmentOrderVo shipmentOrderVo = queryById(id);
        if (shipmentOrderVo == null) {
            throw new BaseException("出库单不存在");
        }
        if (ServiceConstants.ShipmentOrderStatus.FINISH.equals(shipmentOrderVo.getOrderStatus())) {
            throw new ServiceException("删除失败", HttpStatus.CONFLICT,"出库单【" + shipmentOrderVo.getOrderNo() + "】已出库，无法删除！");
        }
    }

    /**
     * 出库
     * @param bo
     */
    @Transactional
    public void shipment(ShipmentOrderBo bo) {
        // 1.校验商品明细不能为空！
        validateBeforeShipment(bo);

        // 1.1 出库单状态设为"出库中"，等AGV回调完成后才更新为"已完成"
        bo.setOrderStatus(ServiceConstants.ShipmentOrderStatus.IN_PROGRESS);

        // 1.5 确保每条明细都有容器号（出库时从起点库位自动查找）
        bo.getDetails().forEach(detail -> {
            if (StringUtils.isBlank(detail.getContainerNo()) && StringUtils.isNotBlank(detail.getSourceLocation())) {
                String lookedUp = locationService.getContainerNoByCode(detail.getSourceLocation());
                if (StringUtils.isNotBlank(lookedUp)) {
                    detail.setContainerNo(lookedUp);
                    log.info("出库单 {} 明细容器号为空，从库位 {} 自动查找：{}", bo.getOrderNo(), detail.getSourceLocation(), lookedUp);
                } else {
                    // 库位也无容器号，自动生成
                    String generated = locationService.generateContainerNo();
                    detail.setContainerNo(generated);
                    log.info("出库单 {} 明细容器号为空且库位无记录，自动生成：{}", bo.getOrderNo(), generated);
                }
            }
        });

        // 2. 保存入库单和入库单明细
        if (Objects.isNull(bo.getId())) {
            insertByBo(bo);
        } else {
            updateByBo(bo);
        }
        // 3.更新库存：Inventory表
        inventoryService.subtract(bo.getDetails());

        // 4.更新库位状态和容器号
        bo.getDetails().forEach(detail -> {
            if (StringUtils.isNotBlank(detail.getSourceLocation())) {
                // 起点库位清空容器
                locationService.updateStatusAndContainerByCode(detail.getSourceLocation(), 0, null);
            }
            if (StringUtils.isNotBlank(detail.getTargetLocation())) {
                // 终点库位记录容器号
                locationService.updateStatusAndContainerByCode(
                    detail.getTargetLocation(), 1, detail.getContainerNo());
            }
        });

        // 5.创建库存记录
        inventoryHistoryService.saveInventoryHistory(bo,ServiceConstants.InventoryHistoryOrderType.SHIPMENT,false);

        // 6.下发 RCS 出库任务（容错：失败不回滚库存）
        rcsTaskHelper.dispatchRcsTask(bo.getOrderNo(), RcsTaskHelper.TASK_TYPE_OUTBOUND, bo.getDetails());
    }


    private void validateBeforeShipment(ShipmentOrderBo bo) {
        if (CollUtil.isEmpty(bo.getDetails())) {
            throw new BaseException("商品明细不能为空！");
        }
    }

    /**
     * 自动完成下一个相同终点的暂存出库单。
     * <p>
     * 当同一出库 taskId 收到 3 条 AGV 回调日志后触发，
     * 查找 orderStatus=0（暂存）且明细 targetLocation 与传入值匹配的出库单，
     * 执行出库完成（扣减库存、更新库位状态、保存库存记录、下发 RCS 任务）。
     *
     * @param targetLocation 终点库位（如 C1、C2），用于匹配暂存出库单
     */
    @Transactional
    public void autoShipNextPendingOrder(String targetLocation) {
        if (StringUtils.isBlank(targetLocation)) {
            log.warn("终点为空，无法匹配暂存出库单");
            return;
        }

        // 查询所有暂存的出库单（orderStatus=0），按创建时间升序
        LambdaQueryWrapper<ShipmentOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(ShipmentOrder::getOrderStatus, ServiceConstants.ShipmentOrderStatus.PENDING);
        lqw.orderByAsc(BaseEntity::getCreateTime);
        List<ShipmentOrderVo> list = shipmentOrderMapper.selectVoList(lqw);

        if (list.isEmpty()) {
            log.info("没有暂存的出库单，无需自动出库");
            return;
        }

        // 找到第一个明细 targetLocation 匹配的出库单
        ShipmentOrderVo matchedVo = null;
        for (ShipmentOrderVo vo : list) {
            List<ShipmentOrderDetailVo> details = shipmentOrderDetailService.queryByShipmentOrderId(vo.getId());
            if (details != null && !details.isEmpty()) {
                for (ShipmentOrderDetailVo detail : details) {
                    if (targetLocation.equals(detail.getTargetLocation())) {
                        matchedVo = vo;
                        break;
                    }
                }
            }
            if (matchedVo != null) {
                break;
            }
        }

        if (matchedVo == null) {
            log.info("没有终点为 {} 的暂存出库单", targetLocation);
            return;
        }

        log.info("自动完成出库单：{}，终点={}", matchedVo.getOrderNo(), targetLocation);

        // 构造 Bo
        ShipmentOrderBo bo = new ShipmentOrderBo();
        bo.setId(matchedVo.getId());
        bo.setOrderNo(matchedVo.getOrderNo());
        bo.setOrderStatus(ServiceConstants.ShipmentOrderStatus.IN_PROGRESS);
        bo.setOptType(matchedVo.getOptType());
        bo.setBizOrderNo(matchedVo.getBizOrderNo());
        bo.setMerchantId(matchedVo.getMerchantId());
        bo.setWarehouseId(matchedVo.getWarehouseId());
        bo.setTotalQuantity(matchedVo.getTotalQuantity());
        bo.setTotalAmount(matchedVo.getTotalAmount());
        bo.setRemark(matchedVo.getRemark());

        // 查询明细并转换为 Bo
        List<ShipmentOrderDetailVo> detailVos = shipmentOrderDetailService.queryByShipmentOrderId(matchedVo.getId());
        List<ShipmentOrderDetailBo> detailBos = new ArrayList<>();
        for (ShipmentOrderDetailVo detailVo : detailVos) {
            ShipmentOrderDetailBo detailBo = new ShipmentOrderDetailBo();
            detailBo.setId(detailVo.getId());
            detailBo.setOrderId(detailVo.getOrderId());
            detailBo.setSkuId(detailVo.getSkuId());
            detailBo.setQuantity(detailVo.getQuantity());
            detailBo.setAmount(detailVo.getAmount());
            detailBo.setRemark(detailVo.getRemark());
            detailBo.setWarehouseId(detailVo.getWarehouseId());
            detailBo.setContainerNo(detailVo.getContainerNo());
            detailBo.setSourceLocation(detailVo.getSourceLocation());
            detailBo.setTargetLocation(detailVo.getTargetLocation());
            detailBos.add(detailBo);
        }
        bo.setDetails(detailBos);

        // 执行出库
        shipment(bo);
        log.info("自动完成出库单 {} 成功", matchedVo.getOrderNo());
    }

    public Long queryIdByOrderNo(String orderNo) {
        ShipmentOrderVo shipmentOrder = shipmentOrderMapper.selectVoOne(new QueryWrapper<ShipmentOrder>().eq("order_no",orderNo));
        return shipmentOrder != null ? shipmentOrder.getId() : null;
    }

    /**
     * 将出库单标记为已完成（AGV回调完成后调用）
     * @param id 出库单ID
     */
    public void markAsFinished(Long id) {
        LambdaUpdateWrapper<ShipmentOrder> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ShipmentOrder::getId, id);
        wrapper.set(ShipmentOrder::getOrderStatus, ServiceConstants.ShipmentOrderStatus.FINISH);
        shipmentOrderMapper.update(null, wrapper);
    }

    /**
     * 手动取消"出库中"的出库单。
     * <p>
     * 将状态从 IN_PROGRESS(2) 回退到 PENDING(0)，并逆转以下操作：
     * 1. 恢复已扣减的库存
     * 2. 恢复起点库位状态（重新占用容器）
     * 3. 清空终点库位状态（释放容器占用）
     * 4. 撤销已下发的 RCS 任务
     *
     * @param id 出库单ID
     */
    @Transactional
    public String cancelInProgress(Long id) {
        ShipmentOrderVo orderVo = queryById(id);
        if (orderVo == null) {
            throw new BaseException("出库单不存在");
        }

        if (!ServiceConstants.ShipmentOrderStatus.IN_PROGRESS.equals(orderVo.getOrderStatus())) {
            throw new ServiceException("取消失败", HttpStatus.CONFLICT,
                "出库单【" + orderVo.getOrderNo() + "】当前状态不允许取消，仅「出库中」状态可取消");
        }

        List<ShipmentOrderDetailVo> detailVos = orderVo.getDetails();
        if (CollUtil.isEmpty(detailVos)) {
            resetOrderStatusToPending(id);
            return "出库单【" + orderVo.getOrderNo() + "】已取消（无明细）";
        }

        // 1. 逆转库存：恢复出库时扣减的库存
        List<ShipmentOrderDetailBo> detailBos = new ArrayList<>();
        for (ShipmentOrderDetailVo detailVo : detailVos) {
            ShipmentOrderDetailBo detailBo = new ShipmentOrderDetailBo();
            detailBo.setSkuId(detailVo.getSkuId());
            detailBo.setWarehouseId(detailVo.getWarehouseId());
            detailBo.setQuantity(detailVo.getQuantity());
            detailBo.setContainerNo(detailVo.getContainerNo());
            detailBo.setSourceLocation(detailVo.getSourceLocation());
            detailBo.setTargetLocation(detailVo.getTargetLocation());
            detailBos.add(detailBo);
        }
        inventoryService.add(detailBos);

        // 2. 恢复库位状态
        detailBos.forEach(detail -> {
            // 起点库位：恢复为占用状态，放回容器
            if (StringUtils.isNotBlank(detail.getSourceLocation()) && StringUtils.isNotBlank(detail.getContainerNo())) {
                locationService.updateStatusAndContainerByCode(
                    detail.getSourceLocation(), 1, detail.getContainerNo());
            }
            // 终点库位：清空容器，释放占用
            if (StringUtils.isNotBlank(detail.getTargetLocation())) {
                locationService.updateStatusAndContainerByCode(
                    detail.getTargetLocation(), 0, null);
            }
        });

        // 3. 撤销 RCS 任务
        String rcsResult = rcsTaskHelper.cancelRcsTask(
            orderVo.getOrderNo(), RcsTaskHelper.TASK_TYPE_OUTBOUND, detailBos);

        // 4. 重置订单状态为暂存
        resetOrderStatusToPending(id);

        log.info("出库单 {} 手动取消完成，RCS：{}", orderVo.getOrderNo(), rcsResult);
        return "出库单【" + orderVo.getOrderNo() + "】已取消，" + rcsResult;
    }

    private void resetOrderStatusToPending(Long id) {
        LambdaUpdateWrapper<ShipmentOrder> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ShipmentOrder::getId, id);
        wrapper.set(ShipmentOrder::getOrderStatus, ServiceConstants.ShipmentOrderStatus.PENDING);
        shipmentOrderMapper.update(null, wrapper);
    }
}

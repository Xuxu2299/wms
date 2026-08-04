package com.ruoyi.wms.rcs;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.constant.ServiceConstants;
import com.ruoyi.common.mybatis.core.domain.BaseEntity;
import com.ruoyi.wms.domain.bo.BaseOrderDetailBo;
import com.ruoyi.wms.domain.bo.ReceiptOrderBo;
import com.ruoyi.wms.domain.bo.ReceiptOrderDetailBo;
import com.ruoyi.wms.domain.bo.ShipmentOrderBo;
import com.ruoyi.wms.domain.bo.ShipmentOrderDetailBo;
import com.ruoyi.wms.domain.entity.ReceiptOrder;
import com.ruoyi.wms.domain.entity.ShipmentOrder;
import com.ruoyi.wms.domain.vo.ReceiptOrderDetailVo;
import com.ruoyi.wms.domain.vo.ReceiptOrderVo;
import com.ruoyi.wms.domain.vo.ShipmentOrderDetailVo;
import com.ruoyi.wms.domain.vo.ShipmentOrderVo;
import com.ruoyi.wms.mapper.ReceiptOrderMapper;
import com.ruoyi.wms.mapper.ShipmentOrderMapper;
import com.ruoyi.wms.service.ReceiptOrderDetailService;
import com.ruoyi.wms.service.ShipmentOrderDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * RCS 任务队列调度器。
 * <p>
 * 保证同一时间只有一个 RCS 任务在执行，避免并发下发导致
 * "容器库位非空闲"等 RCS 侧冲突。
 * <p>
 * 核心流程：
 * 1. receive()/shipment() 提交后，库存立即更新，单据状态设为 WAITING_DISPATCH(3)
 * 2. 调用 tryDispatchNext()：若无 IN_PROGRESS(2) 的同类型单据，则下发最早一条 WAITING_DISPATCH 单据
 * 3. AGV 回调完成后，再次调用 tryDispatchNext() 触发下一个
 * <p>
 * 持久化：WAITING_DISPATCH 状态本身就是数据库标记，后端重启后不丢失，
 * 可通过定时任务或手动触发恢复调度。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RcsTaskDispatcher {

    private final ReceiptOrderMapper receiptOrderMapper;
    private final ShipmentOrderMapper shipmentOrderMapper;
    private final ReceiptOrderDetailService receiptOrderDetailService;
    private final ShipmentOrderDetailService shipmentOrderDetailService;
    private final RcsTaskHelper rcsTaskHelper;

    /**
     * 尝试下发下一个待调度的入库/出库任务。
     * <p>
     * 调度规则：
     * - 若存在 IN_PROGRESS(2) 的入库单，说明当前有任务在 RCS 侧执行，直接返回（等回调触发）
     * - 否则取最早一条 WAITING_DISPATCH(3) 的入库单，下发 RCS 任务，成功后状态改为 IN_PROGRESS(2)
     * - 入库队列和出库队列独立调度，互不阻塞
     * <p>
     * 线程安全：整个方法 synchronized，防止并发调度。
     */
    public synchronized void tryDispatchNextInbound() {
        try {
            // 1. 检查是否有正在执行的入库任务
            LambdaQueryWrapper<ReceiptOrder> inProgressLqw = Wrappers.lambdaQuery();
            inProgressLqw.eq(ReceiptOrder::getOrderStatus, ServiceConstants.ReceiptOrderStatus.IN_PROGRESS);
            long inProgressCount = receiptOrderMapper.selectCount(inProgressLqw);
            if (inProgressCount > 0) {
                log.debug("入库队列：当前有 {} 个任务执行中，等待回调触发", inProgressCount);
                return;
            }

            // 2. 取最早一条待下发的入库单
            LambdaQueryWrapper<ReceiptOrder> waitingLqw = Wrappers.lambdaQuery();
            waitingLqw.eq(ReceiptOrder::getOrderStatus, ServiceConstants.ReceiptOrderStatus.WAITING_DISPATCH);
            waitingLqw.orderByAsc(BaseEntity::getCreateTime);
            List<ReceiptOrderVo> waitingList = receiptOrderMapper.selectVoList(waitingLqw);
            if (waitingList.isEmpty()) {
                log.debug("入库队列：无待下发任务");
                return;
            }

            ReceiptOrderVo nextOrder = waitingList.get(0);
            log.info("入库队列：调度待下发入库单 {}", nextOrder.getOrderNo());

            // 3. 查询明细并构造 BO
            List<ReceiptOrderDetailVo> detailVos = receiptOrderDetailService.queryByReceiptOrderId(nextOrder.getId());
            if (CollUtil.isEmpty(detailVos)) {
                log.warn("入库单 {} 无明细，跳过调度", nextOrder.getOrderNo());
                return;
            }

            List<ReceiptOrderDetailBo> detailBos = new ArrayList<>();
            for (ReceiptOrderDetailVo dv : detailVos) {
                ReceiptOrderDetailBo db = new ReceiptOrderDetailBo();
                db.setId(dv.getId());
                db.setOrderId(dv.getOrderId());
                db.setSkuId(dv.getSkuId());
                db.setQuantity(dv.getQuantity());
                db.setAmount(dv.getAmount());
                db.setRemark(dv.getRemark());
                db.setWarehouseId(dv.getWarehouseId());
                db.setContainerNo(dv.getContainerNo());
                db.setSourceLocation(dv.getSourceLocation());
                db.setTargetLocation(dv.getTargetLocation());
                detailBos.add(db);
            }

            // 4. 下发 RCS 任务
            rcsTaskHelper.dispatchRcsTask(nextOrder.getOrderNo(), RcsTaskHelper.TASK_TYPE_INBOUND, detailBos);

            // 5. 状态改为 IN_PROGRESS(2)
            ReceiptOrder update = new ReceiptOrder();
            update.setId(nextOrder.getId());
            update.setOrderStatus(ServiceConstants.ReceiptOrderStatus.IN_PROGRESS);
            receiptOrderMapper.updateById(update);
            log.info("入库单 {} 已下发 RCS 任务，状态改为入库中", nextOrder.getOrderNo());

        } catch (Exception e) {
            log.error("入库队列调度异常", e);
        }
    }

    /**
     * 尝试下发下一个待调度的出库任务。
     * <p>
     * 逻辑与 tryDispatchNextInbound 对称，针对出库单。
     */
    public synchronized void tryDispatchNextOutbound() {
        try {
            // 1. 检查是否有正在执行的出库任务
            LambdaQueryWrapper<ShipmentOrder> inProgressLqw = Wrappers.lambdaQuery();
            inProgressLqw.eq(ShipmentOrder::getOrderStatus, ServiceConstants.ShipmentOrderStatus.IN_PROGRESS);
            long inProgressCount = shipmentOrderMapper.selectCount(inProgressLqw);
            if (inProgressCount > 0) {
                log.debug("出库队列：当前有 {} 个任务执行中，等待回调触发", inProgressCount);
                return;
            }

            // 2. 取最早一条待下发的出库单
            LambdaQueryWrapper<ShipmentOrder> waitingLqw = Wrappers.lambdaQuery();
            waitingLqw.eq(ShipmentOrder::getOrderStatus, ServiceConstants.ShipmentOrderStatus.WAITING_DISPATCH);
            waitingLqw.orderByAsc(BaseEntity::getCreateTime);
            List<ShipmentOrderVo> waitingList = shipmentOrderMapper.selectVoList(waitingLqw);
            if (waitingList.isEmpty()) {
                log.debug("出库队列：无待下发任务");
                return;
            }

            ShipmentOrderVo nextOrder = waitingList.get(0);
            log.info("出库队列：调度待下发出库单 {}", nextOrder.getOrderNo());

            // 3. 查询明细并构造 BO
            List<ShipmentOrderDetailVo> detailVos = shipmentOrderDetailService.queryByShipmentOrderId(nextOrder.getId());
            if (CollUtil.isEmpty(detailVos)) {
                log.warn("出库单 {} 无明细，跳过调度", nextOrder.getOrderNo());
                return;
            }

            List<ShipmentOrderDetailBo> detailBos = new ArrayList<>();
            for (ShipmentOrderDetailVo dv : detailVos) {
                ShipmentOrderDetailBo db = new ShipmentOrderDetailBo();
                db.setId(dv.getId());
                db.setOrderId(dv.getOrderId());
                db.setSkuId(dv.getSkuId());
                db.setQuantity(dv.getQuantity());
                db.setAmount(dv.getAmount());
                db.setRemark(dv.getRemark());
                db.setWarehouseId(dv.getWarehouseId());
                db.setContainerNo(dv.getContainerNo());
                db.setSourceLocation(dv.getSourceLocation());
                db.setTargetLocation(dv.getTargetLocation());
                detailBos.add(db);
            }

            // 4. 下发 RCS 任务
            rcsTaskHelper.dispatchRcsTask(nextOrder.getOrderNo(), RcsTaskHelper.TASK_TYPE_OUTBOUND, detailBos);

            // 5. 状态改为 IN_PROGRESS(2)
            ShipmentOrder update = new ShipmentOrder();
            update.setId(nextOrder.getId());
            update.setOrderStatus(ServiceConstants.ShipmentOrderStatus.IN_PROGRESS);
            shipmentOrderMapper.updateById(update);
            log.info("出库单 {} 已下发 RCS 任务，状态改为出库中", nextOrder.getOrderNo());

        } catch (Exception e) {
            log.error("出库队列调度异常", e);
        }
    }
}

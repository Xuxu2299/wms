package com.ruoyi.wms.task;

import cn.hutool.core.collection.CollUtil;
import com.ruoyi.common.core.constant.ServiceConstants;
import com.ruoyi.wms.domain.bo.MovementOrderBo;
import com.ruoyi.wms.domain.bo.MovementOrderDetailBo;
import com.ruoyi.wms.domain.vo.OverdueInventoryVo;
import com.ruoyi.wms.domain.vo.StorageDurationRuleVo;
import com.ruoyi.wms.service.MovementOrderService;
import com.ruoyi.wms.service.StorageDurationRuleService;
import com.ruoyi.wms.service.WmsNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 存储时长自动移库定时任务
 * <p>
 * 每小时扫描一次启用的规则，对在源仓库入库时长超过阈值的库存自动生成移库单
 * （状态为已完成），将其从源仓库移至目标仓库，并发送站内消息通知。
 * <p>
 * 说明：自动生成的移库单明细不携带容器号/库位号，因此不会下发 RCS AGV 任务，
 * 仅在账面上完成库存转移。
 *
 * @author wms
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageDurationMovementTask {

    private final StorageDurationRuleService storageDurationRuleService;
    private final MovementOrderService movementOrderService;
    private final WmsNotificationService wmsNotificationService;

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 每小时整点扫描一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void autoMoveByStorageDuration() {
        try {
            List<StorageDurationRuleVo> rules = storageDurationRuleService.queryEnabledRules();
            if (CollUtil.isEmpty(rules)) {
                log.info("存储时长自动移库扫描完成，无启用的规则");
                return;
            }
            int totalMoved = 0;
            for (StorageDurationRuleVo rule : rules) {
                try {
                    totalMoved += processRule(rule);
                } catch (Exception e) {
                    log.error("存储时长自动移库规则[id={}, skuId={}, from={}, to={}]执行异常",
                        rule.getId(), rule.getSkuId(), rule.getFromWarehouseId(), rule.getToWarehouseId(), e);
                }
            }
            log.info("存储时长自动移库扫描完成，共生成 {} 张移库单", totalMoved);
        } catch (Exception e) {
            log.error("存储时长自动移库任务执行异常", e);
        }
    }

    /**
     * 处理单条规则：将命中的超期库存合并为一张移库单移至目标仓库。
     *
     * @return 生成的移库单数量（0 或 1）
     */
    private int processRule(StorageDurationRuleVo rule) {
        List<OverdueInventoryVo> overdueList = storageDurationRuleService.queryOverdueInventory(
            rule.getSkuId(), rule.getFromWarehouseId(), rule.getThresholdDays());
        if (CollUtil.isEmpty(overdueList)) {
            return 0;
        }

        List<MovementOrderDetailBo> details = new ArrayList<>();
        BigDecimal totalQuantity = BigDecimal.ZERO;
        StringBuilder summary = new StringBuilder();
        for (OverdueInventoryVo inv : overdueList) {
            MovementOrderDetailBo detail = new MovementOrderDetailBo();
            detail.setSkuId(inv.getSkuId());
            detail.setQuantity(inv.getQuantity());
            detail.setSourceWarehouseId(rule.getFromWarehouseId());
            detail.setTargetWarehouseId(rule.getToWarehouseId());
            detail.setRemark("超期" + inv.getOverdueDays() + "天自动移库");
            details.add(detail);
            totalQuantity = totalQuantity.add(inv.getQuantity());
            summary.append(String.format("[%s/%s] 数量:%s 超期:%s天; ",
                inv.getItemName(), inv.getSkuName(), inv.getQuantity(), inv.getOverdueDays()));
        }

        MovementOrderBo bo = new MovementOrderBo();
        bo.setOrderNo(generateOrderNo());
        bo.setOrderStatus(ServiceConstants.MovementOrderStatus.FINISH);
        bo.setSourceWarehouseId(rule.getFromWarehouseId());
        bo.setTargetWarehouseId(rule.getToWarehouseId());
        bo.setTotalQuantity(totalQuantity);
        bo.setRemark("存储时长自动移库(规则#" + rule.getId() + ")");
        bo.setDetails(details);

        movementOrderService.move(bo);
        log.warn("存储时长自动移库 | 规则#{} | 移库单:{} | 明细:{} | {}",
            rule.getId(), bo.getOrderNo(), details.size(), summary);

        wmsNotificationService.sendNotification(
            "存储时长自动移库",
            String.format("规则#%s 触发，生成移库单 %s，共 %d 条明细，总数量 %s。明细：%s",
                rule.getId(), bo.getOrderNo(), details.size(), totalQuantity, summary),
            "STORAGE_DURATION_MOVE",
            rule.getId(),
            bo.getOrderNo());

        return 1;
    }

    /**
     * 生成唯一移库单号：YK + 毫秒时间戳 + 随机数
     */
    private String generateOrderNo() {
        return "YK" + LocalDateTime.now().format(NO_FORMATTER) + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}

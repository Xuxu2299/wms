package com.ruoyi.wms.task;

import cn.hutool.core.collection.CollUtil;
import com.ruoyi.wms.domain.vo.InventoryWarningVo;
import com.ruoyi.wms.service.InventoryService;
import com.ruoyi.wms.service.InventorySnapshotService;
import com.ruoyi.wms.service.WmsNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 库存预警定时任务
 * <p>
 * 定期扫描库存预警列表，对低于安全下限或高于安全上限的规格输出告警日志并发送站内消息；
 * 每日凌晨执行一次库存快照。
 *
 * @author wms
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockWarningTask {

    private final InventoryService inventoryService;
    private final WmsNotificationService wmsNotificationService;
    private final InventorySnapshotService inventorySnapshotService;

    /**
     * 每 30 分钟扫描一次库存预警列表。
     * <p>
     * 查询当前总库存低于安全下限或高于安全上限的规格，并以 WARN 级别记录告警信息。
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void checkInventoryWarning() {
        try {
            List<InventoryWarningVo> warningList = inventoryService.queryInventoryWarningList();
            if (CollUtil.isEmpty(warningList)) {
                log.info("库存预警检查完成，未发现预警项");
                return;
            }
            log.warn("库存预警检查完成，共发现 {} 项预警", warningList.size());
            for (InventoryWarningVo warning : warningList) {
                log.warn("库存预警 | 商品名称:{} | 规格名称:{} | 当前库存:{} | 安全下限:{} | 安全上限:{} | 预警类型:{}",
                        warning.getItemName(),
                        warning.getSkuName(),
                        warning.getTotalQuantity(),
                        warning.getMinStock(),
                        warning.getMaxStock(),
                        warning.getWarningType());
                // 发送站内消息通知
                String title = "LOW".equals(warning.getWarningType())
                        ? "库存不足预警"
                        : "库存超限预警";
                String content = String.format("商品[%s] 规格[%s] 当前库存:%s, 安全下限:%s, 安全上限:%s",
                        warning.getItemName(),
                        warning.getSkuName(),
                        warning.getTotalQuantity(),
                        warning.getMinStock(),
                        warning.getMaxStock());
                wmsNotificationService.sendNotification(title, content, "STOCK_WARNING",
                        warning.getSkuId(), null);
            }
        } catch (Exception e) {
            log.error("库存预警检查任务执行异常", e);
        }
    }

    /**
     * 每日凌晨 1 点执行一次库存快照检查。
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void dailySnapshot() {
        try {
            log.info("开始执行每日库存快照");
            inventorySnapshotService.createSnapshot();
            log.info("每日库存快照执行完成");
        } catch (Exception e) {
            log.error("库存每日快照任务执行异常", e);
        }
    }
}

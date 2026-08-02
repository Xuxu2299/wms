package com.ruoyi.wms.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.wms.domain.vo.InventorySnapshotVo;
import com.ruoyi.wms.service.InventorySnapshotService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 库存快照
 *
 * @author zcc
 * @date 2024-07-19
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wms/inventorySnapshot")
public class InventorySnapshotController {

    private final InventorySnapshotService inventorySnapshotService;

    /**
     * 按日期查询库存快照
     *
     * @param date 快照日期
     * @return 快照视图对象列表
     */
    @GetMapping("/list")
    public R<List<InventorySnapshotVo>> list(
        @NotNull(message = "日期不能为空")
        @RequestParam("date")
        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return R.ok(inventorySnapshotService.queryByDate(date));
    }

    /**
     * 查询可用的快照日期列表
     *
     * @return 快照日期列表
     */
    @GetMapping("/dates")
    public R<List<LocalDate>> dates() {
        return R.ok(inventorySnapshotService.listSnapshotDates());
    }

    /**
     * 手动触发库存快照创建
     *
     * @return 操作结果
     */
    @PostMapping("/snapshot")
    public R<Void> snapshot() {
        inventorySnapshotService.createSnapshot();
        return R.ok();
    }

}

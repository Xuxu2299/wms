package com.ruoyi.wms.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.wms.domain.vo.WavePickVo;
import com.ruoyi.wms.service.WavePickService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 波次拣货接口
 *
 * @author wms
 */
@RestController
@RequestMapping("/wms/wavePick")
@RequiredArgsConstructor
public class WavePickController {

    private final WavePickService wavePickService;

    /**
     * 创建波次
     */
    @SaCheckPermission("wms:wavePick:all")
    @PostMapping("/create")
    public R<WavePickVo> create(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> shipmentOrderIds = (List<Long>) body.get("shipmentOrderIds");
        Long warehouseId = body.get("warehouseId") != null
                ? Long.valueOf(body.get("warehouseId").toString())
                : null;
        String remark = body.get("remark") != null ? body.get("remark").toString() : null;
        return R.ok(wavePickService.createWave(shipmentOrderIds, warehouseId, remark));
    }

    /**
     * 查询波次详情
     */
    @SaCheckPermission("wms:wavePick:all")
    @GetMapping("/{id}")
    public R<WavePickVo> getInfo(@PathVariable Long id) {
        return R.ok(wavePickService.queryById(id));
    }

    /**
     * 分页查询波次列表
     */
    @SaCheckPermission("wms:wavePick:all")
    @GetMapping("/list")
    public R<IPage<WavePickVo>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        return R.ok(wavePickService.queryPage(pageNum, pageSize, status));
    }

    /**
     * 标记明细已拣
     */
    @SaCheckPermission("wms:wavePick:all")
    @PutMapping("/pick/{detailId}")
    public R<Void> markPicked(@PathVariable Long detailId) {
        wavePickService.markPicked(detailId);
        return R.ok();
    }

    /**
     * 取消波次
     */
    @SaCheckPermission("wms:wavePick:all")
    @PutMapping("/cancel/{id}")
    public R<Void> cancel(@PathVariable Long id) {
        wavePickService.cancel(id);
        return R.ok();
    }

    /**
     * 删除波次
     */
    @SaCheckPermission("wms:wavePick:all")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        wavePickService.delete(id);
        return R.ok();
    }

}

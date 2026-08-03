package com.ruoyi.wms.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.web.core.BaseController;
import com.ruoyi.wms.domain.vo.LocationInventoryVo;
import com.ruoyi.wms.domain.vo.LocationVo;
import com.ruoyi.wms.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 库位查询接口
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wms/location")
public class LocationController extends BaseController {

    private final LocationService locationService;

    /**
     * 查询 A 区空库位（入库终点可选）
     */
    @SaCheckPermission("wms:location:all")
    @GetMapping("/empty")
    public R<List<LocationVo>> listEmpty() {
        return R.ok(locationService.listEmptyStorage());
    }

    /**
     * 查询 A 区有货库位（出库起点可选）
     */
    @SaCheckPermission("wms:location:all")
    @GetMapping("/occupied")
    public R<List<LocationVo>> listOccupied() {
        return R.ok(locationService.listOccupiedStorage());
    }

    /**
     * 查询入库起点库位（R1/R2）
     */
    @SaCheckPermission("wms:location:all")
    @GetMapping("/receiptStart")
    public R<List<LocationVo>> listReceiptStart() {
        return R.ok(locationService.listReceiptStart());
    }

    /**
     * 查询出库终点库位（C1/C2）
     */
    @SaCheckPermission("wms:location:all")
    @GetMapping("/shipmentEnd")
    public R<List<LocationVo>> listShipmentEnd() {
        return R.ok(locationService.listShipmentEnd());
    }

    /**
     * 查询指定区域的所有库位
     */
    @SaCheckPermission("wms:location:all")
    @GetMapping("/area")
    public R<List<LocationVo>> listByArea(String area) {
        return R.ok(locationService.listByArea(area));
    }

    /**
     * 生成唯一容器号（入库页面自动生成用）。
     */
    @SaCheckPermission("wms:location:all")
    @GetMapping("/generateContainerNo")
    public R<String> generateContainerNo() {
        return R.ok(locationService.generateContainerNo());
    }

    /**
     * 根据库位编码查询容器号（出库页面选择起点库位后自动带出）。
     */
    @SaCheckPermission("wms:location:all")
    @GetMapping("/containerNo")
    public R<String> getContainerNo(String locationCode) {
        return R.ok(locationService.getContainerNoByCode(locationCode));
    }

    /**
     * 查询指定SKU在各库位的剩余库存（出库自动拆分用）。
     * <p>
     * 返回结果按库位编码自然排序（A1 < A2 < A10），
     * 前端可据此从最小库位开始拆分出库数量。
     *
     * @param skuId 规格ID
     */
    @SaCheckPermission("wms:location:all")
    @GetMapping("/inventoryBySku")
    public R<List<LocationInventoryVo>> listInventoryBySku(Long skuId) {
        return R.ok(locationService.listInventoryBySku(skuId));
    }

    /**
     * FEFO推荐出库库位：按效期就近原则推荐出库库位
     */
    @SaCheckPermission("wms:location:all")
    @GetMapping("/fifoRecommend")
    public R<List<LocationInventoryVo>> fifoRecommend(@RequestParam Long skuId) {
        return R.ok(locationService.listFifoRecommend(skuId));
    }

    /**
     * 查询所有库位（管理页面用，仅管理员）
     */
    @SaCheckRole("admin")
    @GetMapping("/listAll")
    public R<List<LocationVo>> listAll() {
        return R.ok(locationService.listAll());
    }

    /**
     * 释放库位（将库位重置为空位，清除容器号，仅管理员）
     * <p>
     * 请求体: { "locationCodes": ["A5", "A6"] }
     */
    @SaCheckRole("admin")
    @Log(title = "释放库位", businessType = BusinessType.UPDATE)
    @PutMapping("/release")
    public R<Integer> release(@RequestBody Map<String, List<String>> body) {
        List<String> locationCodes = body.get("locationCodes");
        if (locationCodes == null || locationCodes.isEmpty()) {
            return R.fail("请选择要释放的库位");
        }
        int count = locationService.releaseLocations(locationCodes);
        return R.ok(count);
    }
}

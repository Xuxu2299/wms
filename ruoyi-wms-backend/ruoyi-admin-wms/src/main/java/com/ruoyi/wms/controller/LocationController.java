package com.ruoyi.wms.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.web.core.BaseController;
import com.ruoyi.wms.domain.vo.LocationInventoryVo;
import com.ruoyi.wms.domain.vo.LocationVo;
import com.ruoyi.wms.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}

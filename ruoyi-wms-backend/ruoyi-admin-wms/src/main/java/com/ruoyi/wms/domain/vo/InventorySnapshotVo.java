package com.ruoyi.wms.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存快照视图对象 wms_inventory_snapshot
 *
 * @author zcc
 * @date 2024-07-19
 */
@Data
public class InventorySnapshotVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 规格ID
     */
    private Long skuId;

    /**
     * 规格名称
     */
    private String skuName;

    /**
     * 商品名称
     */
    private String itemName;

    /**
     * 所属仓库ID
     */
    private Long warehouseId;

    /**
     * 仓库名称
     */
    private String warehouseName;

    /**
     * 库存
     */
    private BigDecimal quantity;

    /**
     * 安全库存下限
     */
    private BigDecimal minStock;

    /**
     * 安全库存上限
     */
    private BigDecimal maxStock;

    /**
     * 快照日期
     */
    private LocalDate snapshotDate;

}

package com.ruoyi.wms.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存快照对象 wms_inventory_snapshot
 *
 * @author zcc
 * @date 2024-07-19
 */
@Data
@TableName("wms_inventory_snapshot")
public class InventorySnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 规格ID
     */
    private Long skuId;

    /**
     * 所属仓库
     */
    private Long warehouseId;

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

    /**
     * 备注
     */
    private String remark;

}

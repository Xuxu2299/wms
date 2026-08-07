package com.ruoyi.wms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 超期库存视图对象：用于存储时长自动移库扫描与预览。
 *
 * @author wms
 */
@Data
public class OverdueInventoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 库存ID
     */
    private Long inventoryId;

    /**
     * 规格ID
     */
    private Long skuId;

    /**
     * 仓库ID（源仓库）
     */
    private Long warehouseId;

    /**
     * 当前库存数量
     */
    private BigDecimal quantity;

    /**
     * 最早入库时间（基于 wms_inventory_history order_type=1 quantity>0 的 MIN(create_time)）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime inboundTime;

    /**
     * 超期天数
     */
    private Long overdueDays;

    /**
     * 规格名称
     */
    private String skuName;

    /**
     * 商品名称
     */
    private String itemName;
}

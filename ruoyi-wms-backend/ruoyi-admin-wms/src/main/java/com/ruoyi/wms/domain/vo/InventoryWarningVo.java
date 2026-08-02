package com.ruoyi.wms.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 库存预警视图对象
 *
 * @author wms
 */
@Data
public class InventoryWarningVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * 当前总库存（各仓库库存之和）
     */
    private BigDecimal totalQuantity;

    /**
     * 安全库存下限
     */
    private BigDecimal minStock;

    /**
     * 安全库存上限
     */
    private BigDecimal maxStock;

    /**
     * 预警类型：LOW 库存不足 / HIGH 库存超限
     */
    private String warningType;
}

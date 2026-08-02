package com.ruoyi.wms.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 库位库存视图对象（按库位维度展示某SKU的剩余库存）
 */
@Data
public class LocationInventoryVo {

    /**
     * 库位编码
     */
    private String locationCode;

    /**
     * 容器号
     */
    private String containerNo;

    /**
     * 剩余库存数量
     */
    private BigDecimal quantity;
}

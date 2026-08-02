package com.ruoyi.wms.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class BaseOrderDetailVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    @ExcelProperty(value = "")
    private Long id;

    /**
     * 入库单号
     */
    @ExcelProperty(value = "订单单号")
    private Long orderId;

    /**
     * 规格id
     */
    @ExcelProperty(value = "规格id")
    private Long skuId;

    /**
     * 入库数量
     */
    @ExcelProperty(value = "数量")
    private BigDecimal quantity;

    /**
     * 金额
     */
    @ExcelProperty(value = "金额")
    private BigDecimal amount;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 所属仓库
     */
    @ExcelProperty(value = "所属仓库")
    private Long warehouseId;

    private ItemSkuVo itemSku;

    private ItemVo item;

    /**
     * 容器号（托盘/周转箱编号，用于 RCS 对接）
     */
    @ExcelProperty(value = "容器号")
    private String containerNo;

    /**
     * 源库位号（RCS node_id，出库/移库时使用）
     */
    @ExcelProperty(value = "源库位号")
    private String sourceLocation;

    /**
     * 目标库位号（RCS node_id，入库/移库时使用）
     */
    @ExcelProperty(value = "目标库位号")
    private String targetLocation;
}

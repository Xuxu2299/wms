package com.ruoyi.wms.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 入库单详情对象 wms_receipt_order_detail
 *
 * @author zcc
 * @date 2024-07-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_receipt_order_detail")
public class ReceiptOrderDetail extends BaseOrderDetail {

    /**
     * 所属仓库
     */
    private Long warehouseId;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 生产日期
     */
    private LocalDate productionDate;

    /**
     * 过期日期
     */
    private LocalDate expiryDate;

}

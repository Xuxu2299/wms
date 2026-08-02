package com.ruoyi.wms.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.ruoyi.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper=true)
public class BaseOrderDetail extends BaseEntity {
    /**
     *
     */
    @TableId(value = "id")
    private Long id;
    /**
     * 入库单号
     */
    private Long orderId;
    /**
     * 规格id
     */
    private Long skuId;
    /**
     * 入库数量
     */
    private BigDecimal quantity;
    /**
     * 金额
     */
    private BigDecimal amount;
    /**
     * 备注
     */
    private String remark;

    /**
     * 容器号（托盘/周转箱编号，用于 RCS 对接）
     */
    private String containerNo;

    /**
     * 源库位号（RCS node_id，出库/移库时使用）
     */
    private String sourceLocation;

    /**
     * 目标库位号（RCS node_id，入库/移库时使用）
     */
    private String targetLocation;
}

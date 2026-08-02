package com.ruoyi.wms.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 波次拣货明细对象 wms_wave_pick_detail
 *
 * @author wms
 */
@Data
@TableName("wms_wave_pick_detail")
public class WavePickDetail implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 波次ID
     */
    private Long waveId;

    /**
     * 出库单ID
     */
    private Long shipmentOrderId;

    /**
     * 出库单号
     */
    private String shipmentOrderNo;

    /**
     * 规格ID
     */
    private Long skuId;

    /**
     * 源库位
     */
    private String sourceLocation;

    /**
     * 容器号
     */
    private String containerNo;

    /**
     * 拣货数量
     */
    private BigDecimal quantity;

    /**
     * 拣货状态：0 待拣 / 1 已拣
     */
    private Integer pickStatus;

}

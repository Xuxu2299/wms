package com.ruoyi.wms.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 波次拣货明细视图对象
 *
 * @author wms
 */
@Data
public class WavePickDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long waveId;
    private Long shipmentOrderId;
    private String shipmentOrderNo;
    private Long skuId;
    private String skuName;
    private String itemName;
    private String sourceLocation;
    private String containerNo;
    private BigDecimal quantity;
    private Integer pickStatus;

}

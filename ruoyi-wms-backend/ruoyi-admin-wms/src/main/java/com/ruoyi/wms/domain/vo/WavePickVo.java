package com.ruoyi.wms.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 波次拣货视图对象
 *
 * @author wms
 */
@Data
public class WavePickVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String waveNo;
    private Long warehouseId;
    private String warehouseName;
    private Integer orderCount;
    private BigDecimal totalQuantity;
    private Integer status;
    private String remark;
    private String createBy;
    private LocalDateTime createTime;
    private List<WavePickDetailVo> details;

}

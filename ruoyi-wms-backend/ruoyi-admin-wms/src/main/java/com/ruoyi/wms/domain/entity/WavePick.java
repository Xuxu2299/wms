package com.ruoyi.wms.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 波次拣货对象 wms_wave_pick
 *
 * @author wms
 */
@Data
@TableName("wms_wave_pick")
public class WavePick implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 波次号
     */
    private String waveNo;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 包含出库单数量
     */
    private Integer orderCount;

    /**
     * 商品总数量
     */
    private BigDecimal totalQuantity;

    /**
     * 状态：0 待拣货 / 1 拣货中 / 2 已完成 / 3 已取消
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}

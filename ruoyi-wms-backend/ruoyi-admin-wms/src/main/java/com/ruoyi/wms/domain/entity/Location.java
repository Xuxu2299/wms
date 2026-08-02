package com.ruoyi.wms.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 库位对象 wms_location
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_location")
public class Location extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /**
     * 库位编号
     */
    private String locationCode;

    /**
     * 所属仓库
     */
    private Long warehouseId;

    /**
     * 区域: A区/R区/C区
     */
    private String area;

    /**
     * 0=空 1=有货
     */
    private Integer status;

    /**
     * 容器号（当前停放在该库位的容器编号）
     */
    private String containerNo;

    /**
     * 备注
     */
    private String remark;
}

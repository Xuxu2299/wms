package com.ruoyi.wms.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 存储时长自动移库规则 wms_storage_duration_rule
 * <p>
 * 当某规格在源仓库的入库时长超过阈值天数时，定时任务自动生成移库单
 * 将其从源仓库移至目标仓库。
 *
 * @author wms
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_storage_duration_rule")
public class StorageDurationRule extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /**
     * 规格ID（为空表示匹配所有规格）
     */
    private Long skuId;

    /**
     * 存储时长阈值（天）
     */
    private Integer thresholdDays;

    /**
     * 源仓库ID
     */
    private Long fromWarehouseId;

    /**
     * 目标仓库ID
     */
    private Long toWarehouseId;

    /**
     * 启用状态：0停用 1启用
     */
    private Integer enableStatus;

    /**
     * 备注
     */
    private String remark;
}

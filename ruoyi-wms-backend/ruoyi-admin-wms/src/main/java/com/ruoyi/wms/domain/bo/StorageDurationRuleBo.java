package com.ruoyi.wms.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.mybatis.core.domain.BaseEntity;
import com.ruoyi.wms.domain.entity.StorageDurationRule;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 存储时长自动移库规则业务对象
 *
 * @author wms
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = StorageDurationRule.class, reverseConvertGenerate = false)
public class StorageDurationRuleBo extends BaseEntity {

    /**
     *
     */
    @NotNull(message = "不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 规格ID（为空表示匹配所有规格）
     */
    private Long skuId;

    /**
     * 存储时长阈值（天）
     */
    @NotNull(message = "存储时长阈值不能为空", groups = { AddGroup.class, EditGroup.class })
    @Positive(message = "存储时长阈值必须大于0", groups = { AddGroup.class, EditGroup.class })
    private Integer thresholdDays;

    /**
     * 源仓库ID
     */
    @NotNull(message = "源仓库不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long fromWarehouseId;

    /**
     * 目标仓库ID
     */
    @NotNull(message = "目标仓库不能为空", groups = { AddGroup.class, EditGroup.class })
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

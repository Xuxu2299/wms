package com.ruoyi.wms.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.mybatis.core.domain.BaseVo;
import com.ruoyi.wms.domain.entity.StorageDurationRule;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;

/**
 * 存储时长自动移库规则视图对象
 *
 * @author wms
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = StorageDurationRule.class)
public class StorageDurationRuleVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @ExcelProperty(value = "")
    private Long id;

    /**
     * 规格ID（为空表示匹配所有规格）
     */
    @ExcelProperty(value = "规格ID")
    private Long skuId;

    /**
     * 存储时长阈值（天）
     */
    @ExcelProperty(value = "存储时长阈值(天)")
    private Integer thresholdDays;

    /**
     * 源仓库ID
     */
    @ExcelProperty(value = "源仓库ID")
    private Long fromWarehouseId;

    /**
     * 目标仓库ID
     */
    @ExcelProperty(value = "目标仓库ID")
    private Long toWarehouseId;

    /**
     * 启用状态：0停用 1启用
     */
    @ExcelProperty(value = "启用状态")
    private Integer enableStatus;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 规格名称（展示用）
     */
    private String skuName;

    /**
     * 商品名称（展示用）
     */
    private String itemName;

    /**
     * 源仓库名称（展示用）
     */
    private String fromWarehouseName;

    /**
     * 目标仓库名称（展示用）
     */
    private String toWarehouseName;
}

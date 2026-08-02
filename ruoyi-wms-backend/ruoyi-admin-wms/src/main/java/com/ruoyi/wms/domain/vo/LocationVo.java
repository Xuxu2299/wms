package com.ruoyi.wms.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.mybatis.core.domain.BaseVo;
import com.ruoyi.wms.domain.entity.Location;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;

/**
 * 库位视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = Location.class)
public class LocationVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "ID")
    private Long id;

    @ExcelProperty(value = "库位编号")
    private String locationCode;

    @ExcelProperty(value = "所属仓库")
    private Long warehouseId;

    @ExcelProperty(value = "区域")
    private String area;

    @ExcelProperty(value = "状态")
    private Integer status;

    @ExcelProperty(value = "容器号")
    private String containerNo;

    @ExcelProperty(value = "备注")
    private String remark;
}

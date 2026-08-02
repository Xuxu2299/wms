package com.ruoyi.wms.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.mybatis.core.domain.BaseVo;
import com.ruoyi.wms.domain.entity.AgvLog;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;

/**
 * AGV 回调日志视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = AgvLog.class)
public class AgvLogVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "ID")
    private Long id;

    @ExcelProperty(value = "任务号")
    private String taskId;

    @ExcelProperty(value = "回调类型")
    private String callbackType;

    @ExcelProperty(value = "站点编码")
    private String siteCode;

    @ExcelProperty(value = "货架号")
    private String rackNo;

    @ExcelProperty(value = "AGV编号")
    private Integer vid;

    @ExcelProperty(value = "货架高度")
    private Double height;

    @ExcelProperty(value = "任务状态")
    private Integer taskStatus;

    @ExcelProperty(value = "返回信息")
    private String returnInfo;

    @ExcelProperty(value = "错误码")
    private String errCode;

    @ExcelProperty(value = "错误类型")
    private Integer errType;

    @ExcelProperty(value = "动作类型")
    private String actionType;

    @ExcelProperty(value = "库位")
    private String location;

    @ExcelProperty(value = "容器编号")
    private String barCode;

    @ExcelProperty(value = "请求报文")
    private String requestBody;

    @ExcelProperty(value = "上报时间戳")
    private Long reportTime;
}

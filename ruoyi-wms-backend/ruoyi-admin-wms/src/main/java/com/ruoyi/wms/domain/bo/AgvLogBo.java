package com.ruoyi.wms.domain.bo;

import com.ruoyi.common.mybatis.core.domain.BaseEntity;
import com.ruoyi.wms.domain.entity.AgvLog;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AGV 回调日志业务对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = AgvLog.class, reverseConvertGenerate = false)
public class AgvLogBo extends BaseEntity {

    /**
     * 任务号
     */
    private String taskId;

    /**
     * 回调类型: TaskAction / AgvSign
     */
    private String callbackType;

    /**
     * 任务状态
     */
    private Integer taskStatus;

    /**
     * AGV编号
     */
    private Integer vid;

    /**
     * 站点编码
     */
    private String siteCode;

    /**
     * 容器编号
     */
    private String barCode;

    private String startTime;
    private String endTime;
}

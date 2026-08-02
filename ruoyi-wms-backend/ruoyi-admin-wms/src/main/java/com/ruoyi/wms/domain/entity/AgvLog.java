package com.ruoyi.wms.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * AGV 回调日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_agv_log")
public class AgvLog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /**
     * 任务号
     */
    private String taskId;

    /**
     * 回调类型: TaskAction / AgvSign
     */
    private String callbackType;

    /**
     * 站点编码
     */
    private String siteCode;

    /**
     * 货架号
     */
    private String rackNo;

    /**
     * AGV编号
     */
    private Integer vid;

    /**
     * 货架高度
     */
    private Double height;

    /**
     * 任务状态: 0=开始执行, 4=完成
     */
    private Integer taskStatus;

    /**
     * 返回信息
     */
    private String returnInfo;

    /**
     * 错误码
     */
    private String errCode;

    /**
     * 错误类型
     */
    private Integer errType;

    /**
     * 安全信号动作类型: 1=放货, 2=取货
     */
    private String actionType;

    /**
     * 安全信号库位
     */
    private String location;

    /**
     * 安全信号容器编号
     */
    private String barCode;

    /**
     * 完整请求报文JSON
     */
    private String requestBody;

    /**
     * RCS上报时间戳
     */
    private Long reportTime;
}

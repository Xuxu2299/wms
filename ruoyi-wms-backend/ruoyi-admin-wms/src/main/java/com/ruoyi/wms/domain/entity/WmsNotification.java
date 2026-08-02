package com.ruoyi.wms.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内消息通知对象 wms_notification
 *
 * @author wms
 */
@Data
@TableName("wms_notification")
public class WmsNotification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型：STOCK_WARNING 库存预警 / AGV_TASK AGV任务 / CHECK_REMIND 盘点提醒 / SYSTEM 系统
     */
    private String notifyType;

    /**
     * 关联业务ID（如订单ID、SKU ID等）
     */
    private Long bizId;

    /**
     * 关联业务编号（如订单号）
     */
    private String bizNo;

    /**
     * 是否已读：0 未读 / 1 已读
     */
    private Integer readStatus;

    /**
     * 接收人用户名（为空表示全体用户）
     */
    private String receiveBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

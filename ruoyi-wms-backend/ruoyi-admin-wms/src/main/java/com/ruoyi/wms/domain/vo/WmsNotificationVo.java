package com.ruoyi.wms.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息通知视图对象
 *
 * @author wms
 */
@Data
public class WmsNotificationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String content;
    private String notifyType;
    private Long bizId;
    private String bizNo;
    private Integer readStatus;
    private String receiveBy;
    private LocalDateTime createTime;

}

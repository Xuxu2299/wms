package com.ruoyi.wms.rcs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RCS 系统对接配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "rcs")
public class RcsProperties {

    /**
     * RCS 网关地址，例如：http://192.168.1.36:9001
     */
    private String baseUrl = "http://192.168.1.36:9001";

    /**
     * HTTP 超时时间，单位毫秒。
     */
    private Integer timeout = 5000;

    /**
     * 默认任务库区。RCS 文档示例：cy、rgv、nz、fs、fb、fr、fork。
     */
    private String defaultDistrict = "cy";

    /**
     * 默认容器类型：1 托盘，2 周转箱，3 纸箱原箱，4 LL面料架，5 空托盘组，6 料架。
     */
    private Integer defaultContainerType = 1;
}

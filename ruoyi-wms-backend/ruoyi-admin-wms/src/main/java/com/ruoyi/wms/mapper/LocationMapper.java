package com.ruoyi.wms.mapper;

import com.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import com.ruoyi.wms.domain.entity.Location;
import com.ruoyi.wms.domain.vo.LocationVo;
import org.apache.ibatis.annotations.Select;

/**
 * 库位 Mapper 接口
 */
public interface LocationMapper extends BaseMapperPlus<Location, LocationVo> {

    /**
     * 查询所有表中最大的容器号（纯数字部分）。
     * <p>
     * 容器号格式为 00001、00002 等递增数字字符串，
     * 从 wms_location、wms_receipt_order_detail、wms_shipment_order_detail、wms_movement_order_detail
     * 四张表中查找最大的数字值，确保不会重复。
     *
     * @return 最大容器号字符串（如 "00005"），无记录返回 null
     */
    @Select("SELECT MAX(CAST(container_no AS UNSIGNED)) FROM (" +
        "SELECT container_no FROM wms_location WHERE container_no REGEXP '^[0-9]+$' AND container_no IS NOT NULL " +
        "UNION ALL " +
        "SELECT container_no FROM wms_receipt_order_detail WHERE container_no REGEXP '^[0-9]+$' AND container_no IS NOT NULL " +
        "UNION ALL " +
        "SELECT container_no FROM wms_shipment_order_detail WHERE container_no REGEXP '^[0-9]+$' AND container_no IS NOT NULL " +
        "UNION ALL " +
        "SELECT container_no FROM wms_movement_order_detail WHERE container_no REGEXP '^[0-9]+$' AND container_no IS NOT NULL " +
        ") t")
    String selectMaxContainerNo();

    /**
     * 从入库明细表中查找指定库位最近一次入库时使用的容器号。
     * <p>
     * 用于库位表 container_no 为空时的兜底查询。
     *
     * @param targetLocation 库位编码（入库时的目标库位）
     * @return 容器号，无则返回 null
     */
    @Select("SELECT container_no FROM wms_receipt_order_detail " +
        "WHERE target_location = #{targetLocation} " +
        "AND container_no IS NOT NULL AND container_no != '' " +
        "ORDER BY create_time DESC LIMIT 1")
    String selectContainerNoFromReceiptDetail(String targetLocation);
}

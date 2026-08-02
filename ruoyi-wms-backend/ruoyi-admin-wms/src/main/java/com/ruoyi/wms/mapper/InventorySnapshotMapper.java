package com.ruoyi.wms.mapper;

import com.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import com.ruoyi.wms.domain.entity.InventorySnapshot;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 库存快照 Mapper 接口
 *
 * @author zcc
 * @date 2024-07-19
 */
public interface InventorySnapshotMapper extends BaseMapperPlus<InventorySnapshot, InventorySnapshot> {

    /**
     * 按日期查询库存快照
     *
     * @param date 快照日期
     * @return 快照列表
     */
    @Select("SELECT * FROM wms_inventory_snapshot WHERE snapshot_date = #{date}")
    List<InventorySnapshot> selectByDate(@Param("date") LocalDate date);

    /**
     * 查询最近的快照日期（最近30天）
     *
     * @return 快照日期列表
     */
    @Select("SELECT DISTINCT snapshot_date FROM wms_inventory_snapshot ORDER BY snapshot_date DESC LIMIT 30")
    List<LocalDate> selectRecentDates();

}

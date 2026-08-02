package com.ruoyi.wms.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import com.ruoyi.wms.domain.bo.InventoryBo;
import com.ruoyi.wms.domain.entity.Inventory;
import com.ruoyi.wms.domain.vo.InventoryVo;
import com.ruoyi.wms.domain.vo.InventoryWarningVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 库存Mapper接口
 *
 * @author zcc
 * @date 2024-07-19
 */
public interface InventoryMapper extends BaseMapperPlus<Inventory, InventoryVo> {

    Page<InventoryVo> queryItemBoardList(Page<InventoryVo> page, @Param("bo") InventoryBo bo);
    Page<InventoryVo> queryWarehouseBoardList(Page<InventoryVo> page, @Param("bo") InventoryBo bo);

    /**
     * 查询库存预警列表：当前总库存低于安全下限或高于安全上限的规格
     */
    List<InventoryWarningVo> queryInventoryWarningList();

}

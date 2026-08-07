package com.ruoyi.wms.mapper;

import com.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import com.ruoyi.wms.domain.entity.StorageDurationRule;
import com.ruoyi.wms.domain.vo.OverdueInventoryVo;
import com.ruoyi.wms.domain.vo.StorageDurationRuleVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 存储时长自动移库规则 Mapper
 *
 * @author wms
 */
public interface StorageDurationRuleMapper extends BaseMapperPlus<StorageDurationRule, StorageDurationRuleVo> {

    /**
     * 查询所有启用的规则
     */
    List<StorageDurationRuleVo> queryEnabledRules();

    /**
     * 查询命中规则的超期库存：某规格在某源仓库入库时长超过阈值且仍有库存。
     *
     * @param skuId           规格ID，为空表示匹配所有规格
     * @param fromWarehouseId 源仓库ID，为空表示匹配所有仓库
     * @param thresholdDays   存储时长阈值（天）
     */
    List<OverdueInventoryVo> queryOverdueInventory(@Param("skuId") Long skuId,
                                                   @Param("fromWarehouseId") Long fromWarehouseId,
                                                   @Param("thresholdDays") Integer thresholdDays);
}

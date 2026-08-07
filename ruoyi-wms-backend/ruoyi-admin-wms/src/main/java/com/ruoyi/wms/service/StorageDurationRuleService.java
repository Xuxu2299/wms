package com.ruoyi.wms.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.MapstructUtils;
import com.ruoyi.common.mybatis.core.page.PageQuery;
import com.ruoyi.common.mybatis.core.page.TableDataInfo;
import com.ruoyi.wms.domain.bo.StorageDurationRuleBo;
import com.ruoyi.wms.domain.bo.WarehouseBo;
import com.ruoyi.wms.domain.entity.StorageDurationRule;
import com.ruoyi.wms.domain.vo.ItemSkuMapVo;
import com.ruoyi.wms.domain.vo.OverdueInventoryVo;
import com.ruoyi.wms.domain.vo.StorageDurationRuleVo;
import com.ruoyi.wms.domain.vo.WarehouseVo;
import com.ruoyi.wms.mapper.StorageDurationRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 存储时长自动移库规则 Service
 *
 * @author wms
 */
@RequiredArgsConstructor
@Service
public class StorageDurationRuleService extends ServiceImpl<StorageDurationRuleMapper, StorageDurationRule> {

    private final StorageDurationRuleMapper storageDurationRuleMapper;
    private final ItemSkuService itemSkuService;
    private final WarehouseService warehouseService;

    /**
     * 查询规则
     */
    public StorageDurationRuleVo queryById(Long id) {
        return storageDurationRuleMapper.selectVoById(id);
    }

    /**
     * 分页查询规则列表
     */
    public TableDataInfo<StorageDurationRuleVo> queryPageList(StorageDurationRuleBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<StorageDurationRule> lqw = buildQueryWrapper(bo);
        Page<StorageDurationRuleVo> result = storageDurationRuleMapper.selectVoPage(pageQuery.build(), lqw);
        enrich(result.getRecords());
        return TableDataInfo.build(result);
    }

    /**
     * 查询规则列表
     */
    public List<StorageDurationRuleVo> queryList(StorageDurationRuleBo bo) {
        LambdaQueryWrapper<StorageDurationRule> lqw = buildQueryWrapper(bo);
        List<StorageDurationRuleVo> list = storageDurationRuleMapper.selectVoList(lqw);
        enrich(list);
        return list;
    }

    /**
     * 填充规格/商品/仓库名称等展示字段
     */
    private void enrich(List<StorageDurationRuleVo> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> skuIds = list.stream()
            .map(StorageDurationRuleVo::getSkuId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, ItemSkuMapVo> skuMap = CollUtil.isEmpty(skuIds)
            ? Map.of()
            : itemSkuService.queryItemSkuMapVosByIds(skuIds);
        Map<Long, String> warehouseMap = warehouseService.queryList(new WarehouseBo()).stream()
            .collect(Collectors.toMap(WarehouseVo::getId, WarehouseVo::getWarehouseName, (a, b) -> a));
        list.forEach(rule -> {
            if (rule.getSkuId() != null) {
                ItemSkuMapVo skuVo = skuMap.get(rule.getSkuId());
                if (skuVo != null) {
                    rule.setSkuName(skuVo.getItemSku() != null ? skuVo.getItemSku().getSkuName() : null);
                    rule.setItemName(skuVo.getItem() != null ? skuVo.getItem().getItemName() : null);
                }
            }
            rule.setFromWarehouseName(warehouseMap.get(rule.getFromWarehouseId()));
            rule.setToWarehouseName(warehouseMap.get(rule.getToWarehouseId()));
        });
    }

    private LambdaQueryWrapper<StorageDurationRule> buildQueryWrapper(StorageDurationRuleBo bo) {
        LambdaQueryWrapper<StorageDurationRule> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getSkuId() != null, StorageDurationRule::getSkuId, bo.getSkuId());
        lqw.eq(bo.getFromWarehouseId() != null, StorageDurationRule::getFromWarehouseId, bo.getFromWarehouseId());
        lqw.eq(bo.getToWarehouseId() != null, StorageDurationRule::getToWarehouseId, bo.getToWarehouseId());
        lqw.eq(bo.getEnableStatus() != null, StorageDurationRule::getEnableStatus, bo.getEnableStatus());
        lqw.orderByDesc(StorageDurationRule::getCreateTime);
        return lqw;
    }

    /**
     * 新增规则
     */
    public void insertByBo(StorageDurationRuleBo bo) {
        validate(bo);
        StorageDurationRule add = MapstructUtils.convert(bo, StorageDurationRule.class);
        if (add.getEnableStatus() == null) {
            add.setEnableStatus(1);
        }
        storageDurationRuleMapper.insert(add);
    }

    /**
     * 修改规则
     */
    public void updateByBo(StorageDurationRuleBo bo) {
        validate(bo);
        StorageDurationRule update = MapstructUtils.convert(bo, StorageDurationRule.class);
        storageDurationRuleMapper.updateById(update);
    }

    private void validate(StorageDurationRuleBo bo) {
        if (Objects.equals(bo.getFromWarehouseId(), bo.getToWarehouseId())) {
            throw new ServiceException("源仓库与目标仓库不能相同");
        }
        if (bo.getThresholdDays() != null && bo.getThresholdDays() <= 0) {
            throw new ServiceException("存储时长阈值必须大于0");
        }
    }

    /**
     * 删除规则
     */
    public void deleteById(Long id) {
        storageDurationRuleMapper.deleteById(id);
    }

    /**
     * 批量删除规则
     */
    public void deleteByIds(Collection<Long> ids) {
        storageDurationRuleMapper.deleteBatchIds(ids);
    }

    /**
     * 查询所有启用的规则
     */
    public List<StorageDurationRuleVo> queryEnabledRules() {
        return storageDurationRuleMapper.queryEnabledRules();
    }

    /**
     * 查询命中规则的超期库存
     */
    public List<OverdueInventoryVo> queryOverdueInventory(Long skuId, Long fromWarehouseId, Integer thresholdDays) {
        return storageDurationRuleMapper.queryOverdueInventory(skuId, fromWarehouseId, thresholdDays);
    }

    /**
     * 预览：查询命中某规则的超期库存
     */
    public List<OverdueInventoryVo> previewOverdueInventory(Long ruleId) {
        StorageDurationRuleVo rule = queryById(ruleId);
        if (rule == null) {
            return CollUtil.newArrayList();
        }
        return queryOverdueInventory(rule.getSkuId(), rule.getFromWarehouseId(), rule.getThresholdDays());
    }
}

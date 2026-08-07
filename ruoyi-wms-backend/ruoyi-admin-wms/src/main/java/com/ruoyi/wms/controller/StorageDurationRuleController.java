package com.ruoyi.wms.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.excel.utils.ExcelUtil;
import com.ruoyi.common.idempotent.annotation.RepeatSubmit;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.mybatis.core.page.PageQuery;
import com.ruoyi.common.mybatis.core.page.TableDataInfo;
import com.ruoyi.common.web.core.BaseController;
import com.ruoyi.wms.domain.bo.StorageDurationRuleBo;
import com.ruoyi.wms.domain.vo.OverdueInventoryVo;
import com.ruoyi.wms.domain.vo.StorageDurationRuleVo;
import com.ruoyi.wms.service.StorageDurationRuleService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 存储时长自动移库规则
 *
 * @author wms
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wms/storageDurationRule")
public class StorageDurationRuleController extends BaseController {

    private final StorageDurationRuleService storageDurationRuleService;

    /**
     * 查询规则列表
     */
    @SaCheckPermission("wms:storageDurationRule:list")
    @GetMapping("/list")
    public TableDataInfo<StorageDurationRuleVo> list(StorageDurationRuleBo bo, PageQuery pageQuery) {
        return storageDurationRuleService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询规则列表（不分页）
     */
    @SaCheckPermission("wms:storageDurationRule:list")
    @GetMapping("/listNoPage")
    public R<List<StorageDurationRuleVo>> listNoPage(StorageDurationRuleBo bo) {
        return R.ok(storageDurationRuleService.queryList(bo));
    }

    /**
     * 导出规则列表
     */
    @SaCheckPermission("wms:storageDurationRule:list")
    @Log(title = "存储时长自动移库规则", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(StorageDurationRuleBo bo, HttpServletResponse response) {
        List<StorageDurationRuleVo> list = storageDurationRuleService.queryList(bo);
        ExcelUtil.exportExcel(list, "存储时长自动移库规则", StorageDurationRuleVo.class, response);
    }

    /**
     * 获取规则详细信息
     */
    @SaCheckPermission("wms:storageDurationRule:list")
    @GetMapping("/{id}")
    public R<StorageDurationRuleVo> getInfo(@NotNull(message = "主键不能为空")
                                            @PathVariable Long id) {
        return R.ok(storageDurationRuleService.queryById(id));
    }

    /**
     * 新增规则
     */
    @SaCheckPermission("wms:storageDurationRule:edit")
    @Log(title = "存储时长自动移库规则", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody StorageDurationRuleBo bo) {
        storageDurationRuleService.insertByBo(bo);
        return R.ok();
    }

    /**
     * 修改规则
     */
    @SaCheckPermission("wms:storageDurationRule:edit")
    @Log(title = "存储时长自动移库规则", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody StorageDurationRuleBo bo) {
        storageDurationRuleService.updateByBo(bo);
        return R.ok();
    }

    /**
     * 删除规则
     */
    @SaCheckPermission("wms:storageDurationRule:edit")
    @Log(title = "存储时长自动移库规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@NotNull(message = "主键不能为空")
                          @PathVariable Long id) {
        storageDurationRuleService.deleteById(id);
        return R.ok();
    }

    /**
     * 预览：查询命中某规则的超期库存（尚未自动移库的库存）
     */
    @SaCheckPermission("wms:storageDurationRule:list")
    @GetMapping("/preview/{id}")
    public R<List<OverdueInventoryVo>> preview(@NotNull(message = "主键不能为空")
                                               @PathVariable Long id) {
        return R.ok(storageDurationRuleService.previewOverdueInventory(id));
    }
}

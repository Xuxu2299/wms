package com.ruoyi.wms.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.wms.domain.vo.WmsNotificationVo;
import com.ruoyi.wms.service.WmsNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 消息通知接口
 *
 * @author wms
 */
@RestController
@RequestMapping("/wms/notification")
@RequiredArgsConstructor
public class WmsNotificationController {

    private final WmsNotificationService wmsNotificationService;

    /**
     * 分页查询通知列表
     */
    @SaCheckPermission("wms:notification:all")
    @GetMapping("/list")
    public R<IPage<WmsNotificationVo>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String notifyType,
            @RequestParam(required = false) Integer readStatus) {
        return R.ok(wmsNotificationService.queryPage(pageNum, pageSize, notifyType, readStatus));
    }

    /**
     * 查询未读消息数量
     */
    @SaCheckPermission("wms:notification:all")
    @GetMapping("/unread/count")
    public R<Long> unreadCount() {
        return R.ok(wmsNotificationService.countUnread());
    }

    /**
     * 标记单条消息为已读
     */
    @SaCheckPermission("wms:notification:all")
    @PutMapping("/read/{id}")
    public R<Void> markAsRead(@PathVariable Long id) {
        wmsNotificationService.markAsRead(id);
        return R.ok();
    }

    /**
     * 标记全部消息为已读
     */
    @SaCheckPermission("wms:notification:all")
    @PutMapping("/readAll")
    public R<Void> markAllAsRead() {
        wmsNotificationService.markAllAsRead();
        return R.ok();
    }

    /**
     * 删除消息
     */
    @SaCheckPermission("wms:notification:all")
    @DeleteMapping("/{ids}")
    public R<Void> delete(@PathVariable Long[] ids) {
        List<Long> idList = Arrays.asList(ids);
        wmsNotificationService.deleteByIds(idList);
        return R.ok();
    }

}

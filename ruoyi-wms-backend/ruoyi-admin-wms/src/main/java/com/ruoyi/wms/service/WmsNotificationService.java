package com.ruoyi.wms.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.wms.domain.entity.WmsNotification;
import com.ruoyi.wms.domain.vo.WmsNotificationVo;
import com.ruoyi.wms.mapper.WmsNotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息通知 Service
 *
 * @author wms
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WmsNotificationService {

    private final WmsNotificationMapper wmsNotificationMapper;

    /**
     * 发送通知（站内消息）
     *
     * @param title      标题
     * @param content    内容
     * @param notifyType 类型
     * @param bizId      关联业务ID
     * @param bizNo      关联业务编号
     */
    public void sendNotification(String title, String content, String notifyType, Long bizId, String bizNo) {
        try {
            WmsNotification notification = new WmsNotification();
            notification.setTitle(title);
            notification.setContent(content);
            notification.setNotifyType(notifyType);
            notification.setBizId(bizId);
            notification.setBizNo(bizNo);
            notification.setReadStatus(0);
            notification.setCreateTime(LocalDateTime.now());
            wmsNotificationMapper.insert(notification);
            log.info("站内消息已发送：{} - {}", title, bizNo);
        } catch (Exception e) {
            log.error("发送站内消息失败", e);
        }
    }

    /**
     * 分页查询通知列表
     */
    public IPage<WmsNotificationVo> queryPage(Integer pageNum, Integer pageSize, String notifyType, Integer readStatus) {
        LambdaQueryWrapper<WmsNotification> lqw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(notifyType)) {
            lqw.eq(WmsNotification::getNotifyType, notifyType);
        }
        if (readStatus != null) {
            lqw.eq(WmsNotification::getReadStatus, readStatus);
        }
        lqw.orderByDesc(WmsNotification::getCreateTime);
        IPage<WmsNotification> page = new Page<>(pageNum, pageSize);
        IPage<WmsNotification> result = wmsNotificationMapper.selectPage(page, lqw);
        // 转换为VO
        IPage<WmsNotificationVo> voPage = result.convert(this::toVo);
        return voPage;
    }

    /**
     * 查询未读消息数量
     */
    public long countUnread() {
        LambdaQueryWrapper<WmsNotification> lqw = new LambdaQueryWrapper<>();
        lqw.eq(WmsNotification::getReadStatus, 0);
        return wmsNotificationMapper.selectCount(lqw);
    }

    /**
     * 标记单条消息为已读
     */
    public void markAsRead(Long id) {
        LambdaUpdateWrapper<WmsNotification> uw = new LambdaUpdateWrapper<>();
        uw.eq(WmsNotification::getId, id);
        uw.set(WmsNotification::getReadStatus, 1);
        wmsNotificationMapper.update(null, uw);
    }

    /**
     * 标记全部消息为已读
     */
    public void markAllAsRead() {
        LambdaUpdateWrapper<WmsNotification> uw = new LambdaUpdateWrapper<>();
        uw.eq(WmsNotification::getReadStatus, 0);
        uw.set(WmsNotification::getReadStatus, 1);
        wmsNotificationMapper.update(null, uw);
    }

    /**
     * 删除消息
     */
    public void deleteByIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        wmsNotificationMapper.deleteBatchIds(ids);
    }

    private WmsNotificationVo toVo(WmsNotification entity) {
        WmsNotificationVo vo = new WmsNotificationVo();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setNotifyType(entity.getNotifyType());
        vo.setBizId(entity.getBizId());
        vo.setBizNo(entity.getBizNo());
        vo.setReadStatus(entity.getReadStatus());
        vo.setReceiveBy(entity.getReceiveBy());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

}

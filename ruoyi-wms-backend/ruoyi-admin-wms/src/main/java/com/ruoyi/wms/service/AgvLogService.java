package com.ruoyi.wms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.mybatis.core.page.PageQuery;
import com.ruoyi.common.mybatis.core.page.TableDataInfo;
import com.ruoyi.wms.domain.bo.AgvLogBo;
import com.ruoyi.wms.domain.entity.AgvLog;
import com.ruoyi.wms.domain.vo.AgvLogVo;
import com.ruoyi.wms.mapper.AgvLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * AGV 回调日志 Service
 */
@RequiredArgsConstructor
@Service
public class AgvLogService extends ServiceImpl<AgvLogMapper, AgvLog> {

    private final AgvLogMapper agvLogMapper;

    /**
     * 保存 AGV 回调日志
     */
    public void saveLog(AgvLog agvLog) {
        agvLogMapper.insert(agvLog);
    }

    /**
     * 统计指定 taskId 的 TaskAction 回调日志数量。
     *
     * @param taskId 任务号
     * @return 该 taskId 的 TaskAction 日志条数
     */
    public long countTaskActionLogs(String taskId) {
        return count(Wrappers.<AgvLog>lambdaQuery()
            .eq(AgvLog::getTaskId, taskId)
            .eq(AgvLog::getCallbackType, "TaskAction"));
    }

    /**
     * 查询日志详情
     */
    public AgvLogVo queryById(Long id) {
        return agvLogMapper.selectVoById(id);
    }

    /**
     * 分页查询日志列表
     */
    public TableDataInfo<AgvLogVo> queryPageList(AgvLogBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<AgvLog> lqw = buildQueryWrapper(bo);
        Page<AgvLogVo> result = agvLogMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询日志列表（不分页）
     */
    public List<AgvLogVo> queryList(AgvLogBo bo) {
        LambdaQueryWrapper<AgvLog> lqw = buildQueryWrapper(bo);
        return agvLogMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<AgvLog> buildQueryWrapper(AgvLogBo bo) {
        LambdaQueryWrapper<AgvLog> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getTaskId() != null, AgvLog::getTaskId, bo.getTaskId());
        lqw.eq(bo.getCallbackType() != null, AgvLog::getCallbackType, bo.getCallbackType());
        lqw.eq(bo.getTaskStatus() != null, AgvLog::getTaskStatus, bo.getTaskStatus());
        lqw.eq(bo.getVid() != null, AgvLog::getVid, bo.getVid());
        lqw.eq(bo.getSiteCode() != null, AgvLog::getSiteCode, bo.getSiteCode());
        lqw.eq(bo.getBarCode() != null, AgvLog::getBarCode, bo.getBarCode());
        lqw.orderByDesc(AgvLog::getCreateTime);
        return lqw;
    }

    /**
     * 批量删除日志
     */
    public void deleteByIds(Collection<Long> ids) {
        agvLogMapper.deleteBatchIds(ids);
    }

    /**
     * 清空所有日志
     */
    public void clean() {
        agvLogMapper.delete(Wrappers.lambdaQuery());
    }
}

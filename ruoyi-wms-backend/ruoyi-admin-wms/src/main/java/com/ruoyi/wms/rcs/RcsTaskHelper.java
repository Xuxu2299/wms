package com.ruoyi.wms.rcs;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.wms.domain.bo.BaseOrderDetailBo;
import com.ruoyi.wms.service.AgvLogService;
import com.ruoyi.wms.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * RCS 任务下发辅助类。
 * <p>
 * 将 WMS 出库/入库/移库业务单据转换为 RCS 任务请求并下发。
 * 采用容错策略：RCS 调用失败时仅记录日志，不回滚已完成的库存事务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RcsTaskHelper {

    private final RcsClientService rcsClientService;
    private final LocationService locationService;
    private final AgvLogService agvLogService;

    /**
     * RCS 任务类型常量
     */
    public static final int TASK_TYPE_INBOUND = 0;
    public static final int TASK_TYPE_OUTBOUND = 1;
    public static final int TASK_TYPE_MOVE = 2;

    /**
     * 根据业务明细构建并下发 RCS 任务。
     * <p>
     * 根据 RCS 接口文档："一个拆叠盘位只支持一个任务下发，执行完成再继续下发"，
     * 因此每次只下发第一个任务（序号1），后续任务在 AGV 回调3条日志后
     * 由 dispatchNextTask 自动续发。
     *
     * @param orderNo   WMS 单据号（用于生成 groupId 和 taskId）
     * @param taskType  RCS 任务类型：0=入库, 1=出库, 2=移库
     * @param details   业务明细列表
     * @param <T>       明细 BO 类型
     */
    public <T extends BaseOrderDetailBo> void dispatchRcsTask(String orderNo, int taskType, List<T> details) {
        if (CollUtil.isEmpty(details)) {
            return;
        }

        // 只下发第一个需要执行的任务（序号1）
        int seq = 1;
        for (T detail : details) {
            // 只有填写了容器号或库位号的明细才下发 RCS 任务
            if (StrUtil.isBlank(detail.getContainerNo())
                && StrUtil.isBlank(detail.getSourceLocation())
                && StrUtil.isBlank(detail.getTargetLocation())) {
                continue;
            }

            // 只下发第一个任务，后续任务等回调完成后由 dispatchNextTask 续发
            RcsModels.TaskItem item = buildTaskItem(orderNo, taskType, detail, seq);

            // 检查 taskId 是否已下发过，避免重复下发导致 AGV 平台报错
            if (agvLogService.hasTaskId(item.getTaskId())) {
                log.warn("单据 {} 的 RCS 任务 {} 已存在，跳过重复下发", orderNo, item.getTaskId());
                return;
            }

            RcsModels.TaskReceiveRequest request = new RcsModels.TaskReceiveRequest();
            request.setGroupId(orderNo);
            request.setTasks(new ArrayList<>(List.of(item)));

            try {
                RcsModels.RcsResponse response = rcsClientService.taskReceive(request);
                if (response.getSuccess() != null && response.getSuccess()) {
                    log.info("单据 {} 下发首个 RCS 任务 {} 成功（后续任务待回调后续发）", orderNo, item.getTaskId());
                } else {
                    log.warn("单据 {} 下发 RCS 任务 {} 返回失败：{}", orderNo, item.getTaskId(),
                        response.getData() != null ? response.getData().getReturnInfo() : "未知错误");
                }
            } catch (Exception e) {
                // RCS 调用失败不阻断主业务流程，仅记录日志
                log.error("单据 {} RCS 任务下发异常：{}", orderNo, e.getMessage(), e);
            }
            return;
        }

        log.info("单据 {} 无需下发 RCS 任务（未填写容器号/库位号）", orderNo);
    }

    /**
     * 根据业务明细撤销已下发的 RCS 任务。
     * <p>
     * 按照与 dispatchRcsTask 相同的规则重建 taskId，逐个调用 RCS 取消接口。
     *
     * @param orderNo   WMS 单据号
     * @param taskType  RCS 任务类型：0=入库, 1=出库, 2=移库
     * @param details   业务明细列表
     * @param <T>       明细 BO 类型
     * @return 撤销结果摘要
     */
    public <T extends BaseOrderDetailBo> String cancelRcsTask(String orderNo, int taskType, List<T> details) {
        if (CollUtil.isEmpty(details)) {
            return "无明细，无需撤销";
        }

        int seq = 1;
        int successCount = 0;
        int failCount = 0;

        for (T detail : details) {
            // 与下发逻辑一致：只有填写了容器号或库位号的明细才撤销
            if (StrUtil.isBlank(detail.getContainerNo())
                && StrUtil.isBlank(detail.getSourceLocation())
                && StrUtil.isBlank(detail.getTargetLocation())) {
                continue;
            }

            String taskId = orderNo + "-" + String.format("%03d", seq);
            RcsModels.CancelTaskRequest cancelRequest = new RcsModels.CancelTaskRequest();
            cancelRequest.setTaskId(taskId);

            try {
                RcsModels.RcsResponse response = rcsClientService.cancelTask(cancelRequest);
                if (response.getSuccess() != null && response.getSuccess()) {
                    successCount++;
                    log.info("单据 {} 撤销 RCS 任务 {} 成功", orderNo, taskId);
                } else {
                    failCount++;
                    log.warn("单据 {} 撤销 RCS 任务 {} 返回失败：{}", orderNo, taskId,
                        response.getData() != null ? response.getData().getReturnInfo() : "未知错误");
                }
            } catch (Exception e) {
                failCount++;
                log.error("单据 {} 撤销 RCS 任务 {} 异常：{}", orderNo, taskId, e.getMessage(), e);
            }
            seq++;
        }

        String summary = String.format("撤销完成：成功 %d 个，失败 %d 个", successCount, failCount);
        log.info("单据 {} 撤销 RCS 任务完成：{}", orderNo, summary);
        return summary;
    }

    /**
     * 下发单个 RCS 任务（用于任务完成后自动下发下一个）。
     *
     * @param orderNo  WMS 单据号
     * @param taskType RCS 任务类型：0=入库, 1=出库, 2=移库
     * @param detail   业务明细
     * @param seq      任务序号（与 dispatchRcsTask 中的编号规则一致）
     */
    public void dispatchSingleTask(String orderNo, int taskType, BaseOrderDetailBo detail, int seq) {
        if (detail == null) {
            return;
        }

        RcsModels.TaskItem item = buildTaskItem(orderNo, taskType, detail, seq);

        // 检查 taskId 是否已下发过，避免重复下发导致 AGV 平台报错
        if (agvLogService.hasTaskId(item.getTaskId())) {
            log.warn("单据 {} 的 RCS 任务 {} 已存在，跳过重复下发", orderNo, item.getTaskId());
            return;
        }

        RcsModels.TaskReceiveRequest request = new RcsModels.TaskReceiveRequest();
        request.setGroupId(orderNo);
        request.setTasks(new ArrayList<>(List.of(item)));

        try {
            RcsModels.RcsResponse response = rcsClientService.taskReceive(request);
            if (response.getSuccess() != null && response.getSuccess()) {
                log.info("单据 {} 自动下发下一个 RCS 任务 {} 成功", orderNo, item.getTaskId());
            } else {
                log.warn("单据 {} 自动下发 RCS 任务 {} 返回失败：{}", orderNo, item.getTaskId(),
                    response.getData() != null ? response.getData().getReturnInfo() : "未知错误");
            }
        } catch (Exception e) {
            log.error("单据 {} 自动下发 RCS 任务 {} 异常：{}", orderNo, item.getTaskId(), e.getMessage(), e);
        }
    }

    /**
     * 判断明细是否需要下发 RCS 任务（与 dispatchRcsTask 中的过滤逻辑一致）。
     *
     * @param detail 业务明细
     * @return true 表示需要下发
     */
    public static boolean needDispatch(BaseOrderDetailBo detail) {
        return StrUtil.isNotBlank(detail.getContainerNo())
            || StrUtil.isNotBlank(detail.getSourceLocation())
            || StrUtil.isNotBlank(detail.getTargetLocation());
    }

    /**
     * 构建单个 RCS 任务项。
     * <p>
     * 确保容器号不为空：
     * - 出库/移库场景：容器号为空时，从起点库位自动查找容器号
     * - 入库场景：容器号为空时，自动生成唯一容器号
     *
     * @param orderNo  单据号
     * @param taskType 任务类型
     * @param detail   业务明细
     * @param seq      序号
     * @return RCS 任务项
     */
    private RcsModels.TaskItem buildTaskItem(String orderNo, int taskType, BaseOrderDetailBo detail, int seq) {
        String containerNo = detail.getContainerNo();

        // 容器号为空时的自动补全逻辑
        if (StrUtil.isBlank(containerNo)) {
            if (StrUtil.isNotBlank(detail.getSourceLocation())) {
                // 出库/移库场景：从起点库位自动查找容器号
                String lookedUp = locationService.getContainerNoByCode(detail.getSourceLocation());
                if (StrUtil.isNotBlank(lookedUp)) {
                    containerNo = lookedUp;
                    log.info("明细容器号为空，从库位 {} 自动查找容器号：{}", detail.getSourceLocation(), lookedUp);
                }
            }
            // 仍然为空，自动生成新容器号（入库场景或库位无容器号时）
            if (StrUtil.isBlank(containerNo)) {
                containerNo = locationService.generateContainerNo();
                log.info("明细容器号为空，自动生成新容器号：{}", containerNo);
            }
            // 回填到明细对象，确保后续保存和日志一致
            detail.setContainerNo(containerNo);
        }

        RcsModels.TaskItem item = new RcsModels.TaskItem();
        item.setTaskId(orderNo + "-" + String.format("%03d", seq));
        item.setTaskType(taskType);
        item.setPriorityCode(100);
        item.setBarCode(StrUtil.blankToDefault(containerNo, ""));
        item.setStartNode(StrUtil.blankToDefault(detail.getSourceLocation(), ""));
        item.setEndNode(StrUtil.blankToDefault(detail.getTargetLocation(), ""));
        item.setFromArea(0);
        item.setToArea(0);
        return item;
    }
}

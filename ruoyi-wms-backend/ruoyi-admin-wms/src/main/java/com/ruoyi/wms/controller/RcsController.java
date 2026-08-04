package com.ruoyi.wms.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.ratelimiter.annotation.RateLimiter;
import com.ruoyi.common.ratelimiter.enums.LimitType;
import com.ruoyi.wms.domain.bo.BaseOrderDetailBo;
import com.ruoyi.wms.domain.entity.AgvLog;
import com.ruoyi.wms.domain.vo.ReceiptOrderVo;
import com.ruoyi.wms.domain.vo.ShipmentOrderVo;
import com.ruoyi.wms.domain.vo.MovementOrderVo;
import com.ruoyi.wms.domain.vo.BaseOrderDetailVo;
import com.ruoyi.wms.rcs.RcsClientService;
import com.ruoyi.wms.rcs.RcsModels;
import com.ruoyi.wms.rcs.RcsTaskHelper;
import com.ruoyi.wms.service.AgvLogService;
import com.ruoyi.wms.service.ReceiptOrderService;
import com.ruoyi.wms.service.ShipmentOrderService;
import com.ruoyi.wms.service.MovementOrderService;
import com.ruoyi.wms.service.WmsNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RCS 系统对接接口。
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wms/rcs")
public class RcsController {

    private final RcsClientService rcsClientService;
    private final AgvLogService agvLogService;
    private final RcsTaskHelper rcsTaskHelper;
    private final ReceiptOrderService receiptOrderService;
    private final ShipmentOrderService shipmentOrderService;
    private final MovementOrderService movementOrderService;
    private final WmsNotificationService wmsNotificationService;
    private final com.ruoyi.wms.rcs.RcsTaskDispatcher rcsTaskDispatcher;

    /**
     * WMS 向 RCS 下发任务。
     */
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping({"/taskReceive", "/task"})
    public R<RcsModels.RcsResponse> taskReceive(@RequestBody RcsModels.TaskReceiveRequest request) {
        return R.ok(rcsClientService.taskReceive(request));
    }

    /**
     * WMS 修改 RCS 任务优先级。
     */
    @PostMapping({"/modTask", "/modPriority"})
    public R<RcsModels.RcsResponse> modifyPriority(@RequestBody RcsModels.ModifyPriorityRequest request) {
        return R.ok(rcsClientService.modifyPriority(request));
    }

    /**
     * WMS 取消 RCS 未执行任务（单个 taskId）。
     */
    @PostMapping({"/cancelTask", "/cancel"})
    public R<RcsModels.RcsResponse> cancelTask(@RequestBody RcsModels.CancelTaskRequest request) {
        return R.ok(rcsClientService.cancelTask(request));
    }

    /**
     * WMS 按订单号批量撤销 RCS 任务。
     * <p>
     * 前端传入订单号、任务类型和明细列表，后端按照与下发时相同的规则
     * 重建 taskId 并逐个调用 RCS 取消接口。
     *
     * @param request 包含 orderNo、taskType、details 的请求体
     * @return 撤销结果摘要
     */
    @PostMapping("/cancelTaskByOrder")
    public R<String> cancelTaskByOrder(@RequestBody RcsModels.CancelTaskByOrderRequest request) {
        if (request.getDetails() == null) {
            request.setDetails(java.util.Collections.emptyList());
        }
        // 将 CancelTaskDetailInfo 转换为 RcsTaskHelper 可识别的 BaseOrderDetailBo 子类
        List<com.ruoyi.wms.domain.bo.BaseOrderDetailBo> details = new java.util.ArrayList<>();
        for (RcsModels.CancelTaskDetailInfo info : request.getDetails()) {
            com.ruoyi.wms.domain.bo.BaseOrderDetailBo bo = new com.ruoyi.wms.domain.bo.BaseOrderDetailBo();
            bo.setContainerNo(info.getContainerNo());
            bo.setSourceLocation(info.getSourceLocation());
            bo.setTargetLocation(info.getTargetLocation());
            details.add(bo);
        }
        String summary = rcsTaskHelper.cancelRcsTask(
            request.getOrderNo(),
            request.getTaskType() == null ? 0 : request.getTaskType(),
            details
        );
        return R.ok(summary);
    }

    /**
     * RCS 回调 WMS：任务状态上报。
     *
     * 文档中的回调地址示例为 /xxx/TaskAction。现场可配置为：
     * http://WMS地址:8080/wms/rcs/TaskAction
     *
     * 注意：部分 RCS 系统会在配置的回调地址后自动追加 /TaskAction，
     * 导致实际请求路径变为 /wms/rcs/TaskAction/TaskAction，
     * 因此额外兼容该重复路径。
     */
    @SaIgnore
    @PostMapping({"/TaskAction", "/taskAction", "/taskStatus",
                   "/TaskAction/TaskAction", "/taskAction/taskAction"})
    public RcsModels.CallbackResponse taskAction(@RequestBody RcsModels.TaskStatusReport report) {
        log.info("收到 RCS 任务状态上报：taskId={}, taskStatus={}, vid={}, siteCode={}",
            report.getTaskId(), report.getTaskStatus(), report.getVid(), report.getSiteCode());
        saveTaskActionLog(report);

        // 统计相同 taskId 的 TaskAction 回调日志数量
        // 当达到 3 条时，先尝试下发同单据的下一个任务（一个拆叠盘位只支持一个任务）；
        // 如果同单据没有下一个任务了，再触发下一个暂存单：
        // - 出库任务(CK)：删除终点容器，然后自动完成下一个相同终点的暂存出库单
        // - 入库任务(RK)：自动完成下一个相同起点的暂存入库单
        if (StrUtil.isNotBlank(report.getTaskId())) {
            long callbackCount = agvLogService.countTaskActionLogs(report.getTaskId());
            log.info("taskId {} 当前已收到 {} 条回调日志", report.getTaskId(), callbackCount);
            if (callbackCount >= 3) {
                String taskId = report.getTaskId();
                // 先尝试下发同单据的下一个任务
                boolean hasNextTask = dispatchNextTask(taskId);
                if (!hasNextTask) {
                    // 同单据所有任务已完成，更新当前订单状态为"已完成"
                    log.info("单据 {} 所有任务已完成，更新订单状态为已完成", taskId);
                    String orderNo = taskId.substring(0, taskId.lastIndexOf('-'));
                    if (taskId.startsWith("CK")) {
                        // 出库任务完成
                        Long shipId = shipmentOrderService.queryIdByOrderNo(orderNo);
                        if (shipId != null) {
                            shipmentOrderService.markAsFinished(shipId);
                            log.info("出库单 {} 状态已更新为已完成", orderNo);
                            wmsNotificationService.sendNotification(
                                    "出库任务完成",
                                    "出库单 " + orderNo + " 的AGV任务已全部完成，订单状态已更新为已完成",
                                    "AGV_TASK", shipId, orderNo);
                        }
                        // 删除终点容器
                        try {
                            deleteDestinationContainers(taskId);
                        } catch (Exception e) {
                            log.error("删除终点容器异常，taskId：{}", taskId, e);
                        }
                        // 自动完成下一个相同终点的暂存出库单
                        String targetLocation = resolveTargetLocation(taskId);
                        if (StrUtil.isNotBlank(targetLocation)) {
                            log.info("taskId {} 终点={}, 触发自动完成下一个暂存出库单",
                                taskId, targetLocation);
                            try {
                                shipmentOrderService.autoShipNextPendingOrder(targetLocation);
                            } catch (Exception e) {
                                log.error("自动完成下一个暂存出库单异常，taskId：{}", taskId, e);
                            }
                        } else {
                            log.warn("taskId {} 无法解析终点，跳过自动出库", taskId);
                        }
                        // 触发调度器：尝试下发下一个待下发的出库单（队列机制）
                        try {
                            rcsTaskDispatcher.tryDispatchNextOutbound();
                        } catch (Exception e) {
                            log.error("回调后触发出库调度器异常，taskId：{}", taskId, e);
                        }
                    } else if (taskId.startsWith("RK")) {
                        // 入库任务完成
                        Long receiptId = receiptOrderService.queryIdByOrderNo(orderNo);
                        if (receiptId != null) {
                            receiptOrderService.markAsFinished(receiptId);
                            log.info("入库单 {} 状态已更新为已完成", orderNo);
                            wmsNotificationService.sendNotification(
                                    "入库任务完成",
                                    "入库单 " + orderNo + " 的AGV任务已全部完成，订单状态已更新为已完成",
                                    "AGV_TASK", receiptId, orderNo);
                        }
                        // 自动完成下一个相同起点的暂存入库单
                        String sourceLocation = resolveSourceLocation(taskId);
                        if (StrUtil.isNotBlank(sourceLocation)) {
                            log.info("taskId {} 起点={}, 触发自动完成下一个暂存入库单",
                                taskId, sourceLocation);
                            try {
                                receiptOrderService.autoReceiveNextPendingOrder(sourceLocation);
                            } catch (Exception e) {
                                log.error("自动完成下一个暂存入库单异常，taskId：{}", taskId, e);
                            }
                        } else {
                            log.warn("taskId {} 无法解析起点，跳过自动入库", taskId);
                        }
                        // 触发调度器：尝试下发下一个待下发的入库单（队列机制）
                        try {
                            rcsTaskDispatcher.tryDispatchNextInbound();
                        } catch (Exception e) {
                            log.error("回调后触发入库调度器异常，taskId：{}", taskId, e);
                        }
                    }
                }
            }
        }

        return RcsModels.CallbackResponse.ok();
    }

    /**
     * RCS 回调 WMS：请求安全信号。
     *
     * 文档中的回调地址示例为 /xxx/AgvSign。现场可配置为：
     * http://WMS地址:8080/wms/rcs/AgvSign
     *
     * 注意：部分 RCS 系统会在配置的回调地址后自动追加 /AgvSign，
     * 导致实际请求路径变为 /wms/rcs/AgvSign/AgvSign，
     * 因此额外兼容该重复路径。
     */
    @SaIgnore
    @PostMapping({"/AgvSign", "/agvSign", "/safetySign",
                   "/AgvSign/AgvSign", "/agvSign/agvSign"})
    public RcsModels.AgvSignResponse agvSign(@RequestBody RcsModels.AgvSignRequest request) {
        log.info("收到 RCS 安全信号请求：{}", request);
        saveAgvSignLog(request);
        return RcsModels.AgvSignResponse.allow();
    }

    /**
     * 保存任务状态上报日志
     */
    private void saveTaskActionLog(RcsModels.TaskStatusReport report) {
        try {
            AgvLog agvLog = new AgvLog();
            agvLog.setCallbackType("TaskAction");
            agvLog.setTaskId(report.getTaskId());
            agvLog.setSiteCode(report.getSiteCode());
            agvLog.setRackNo(report.getRackNo());
            agvLog.setVid(report.getVid());
            agvLog.setHeight(report.getHeight() != null ? report.getHeight().doubleValue() : null);
            agvLog.setTaskStatus(report.getTaskStatus());
            agvLog.setReturnInfo(report.getReturnInfo());
            agvLog.setErrCode(report.getErrCode());
            agvLog.setErrType(report.getErrType());
            agvLog.setReportTime(report.getReportTime());
            agvLog.setRequestBody(JSONUtil.toJsonStr(report));
            agvLogService.saveLog(agvLog);
        } catch (Exception e) {
            log.error("保存 AGV 任务状态日志失败", e);
        }
    }

    /**
     * 任务完成后自动下发同一单据的下一个 RCS 任务。
     * <p>
     * 解析已完成的 taskId（格式：单据号-序号，如 RK07309690-001），
     * 根据单据号前缀判断单据类型，查询明细列表，
     * 按照与 dispatchRcsTask 相同的编号规则找到下一个需要下发的任务。
     *
     * @param completedTaskId 已完成的 taskId
     * @return true 表示已下发下一个任务，false 表示该单据所有任务已全部完成
     */
    private boolean dispatchNextTask(String completedTaskId) {
        if (StrUtil.isBlank(completedTaskId)) {
            return false;
        }

        int dashIndex = completedTaskId.lastIndexOf('-');
        if (dashIndex < 0) {
            log.warn("无法解析 taskId：{}", completedTaskId);
            return false;
        }

        String orderNo = completedTaskId.substring(0, dashIndex);
        int completedSeq;
        try {
            completedSeq = Integer.parseInt(completedTaskId.substring(dashIndex + 1));
        } catch (NumberFormatException e) {
            log.warn("无法解析 taskId 序号：{}", completedTaskId);
            return false;
        }
        int nextSeq = completedSeq + 1;

        log.info("任务 {} 已完成，尝试自动下发下一个任务（序号 {}）", completedTaskId, nextSeq);

        try {
            // 根据单据号前缀判断单据类型：RK=入库, CK=出库, YK=移库
            List<? extends BaseOrderDetailVo> details;
            int taskType;

            if (orderNo.startsWith("RK")) {
                Long id = receiptOrderService.queryIdByOrderNo(orderNo);
                if (id == null) {
                    log.warn("未找到入库单：{}", orderNo);
                    return false;
                }
                ReceiptOrderVo vo = receiptOrderService.queryById(id);
                details = vo.getDetails();
                taskType = RcsTaskHelper.TASK_TYPE_INBOUND;
            } else if (orderNo.startsWith("CK")) {
                Long id = shipmentOrderService.queryIdByOrderNo(orderNo);
                if (id == null) {
                    log.warn("未找到出库单：{}", orderNo);
                    return false;
                }
                ShipmentOrderVo vo = shipmentOrderService.queryById(id);
                details = vo.getDetails();
                taskType = RcsTaskHelper.TASK_TYPE_OUTBOUND;
            } else if (orderNo.startsWith("YK")) {
                // 移库单没有 queryIdByOrderNo，遍历查询
                MovementOrderVo vo = findMovementOrderByNo(orderNo);
                if (vo == null) {
                    log.warn("未找到移库单：{}", orderNo);
                    return false;
                }
                details = vo.getDetails();
                taskType = RcsTaskHelper.TASK_TYPE_MOVE;
            } else {
                log.warn("无法识别单据类型，taskId：{}", completedTaskId);
                return false;
            }

            if (details == null || details.isEmpty()) {
                log.info("单据 {} 无明细，无需下发下一个任务", orderNo);
                return false;
            }

            // 按照与 dispatchRcsTask 相同的规则遍历明细，找到序号为 nextSeq 的任务
            int seq = 1;
            for (BaseOrderDetailVo detail : details) {
                if (!RcsTaskHelper.needDispatch(toBo(detail))) {
                    continue;
                }
                if (seq == nextSeq) {
                    rcsTaskHelper.dispatchSingleTask(orderNo, taskType, toBo(detail), seq);
                    return true;
                }
                seq++;
            }

            // 没有下一个任务了
            log.info("单据 {} 的所有 RCS 任务已全部完成（已完成序号 {}）", orderNo, completedSeq);
            return false;

        } catch (Exception e) {
            log.error("自动下发下一个任务异常，taskId：{}", completedTaskId, e);
            return false;
        }
    }

    /**
     * 出库任务完成后，删除终点容器（C1、C2）。
     * <p>
     * 解析出库单号，查询出库单明细，
     * 对每条明细的终点库位（targetLocation，如 C1、C2），
     * 调用 RCS editRackLocation 接口（type=2 删除），
     * 将该位置的容器从 RCS 系统中删除。
     *
     * @param taskId 已完成的出库任务号（如 CK07309690-001）
     */
    private void deleteDestinationContainers(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return;
        }

        int dashIndex = taskId.lastIndexOf('-');
        if (dashIndex < 0) {
            log.warn("无法解析 taskId：{}", taskId);
            return;
        }

        String orderNo = taskId.substring(0, dashIndex);

        try {
            Long id = shipmentOrderService.queryIdByOrderNo(orderNo);
            if (id == null) {
                log.warn("未找到出库单：{}", orderNo);
                return;
            }
            ShipmentOrderVo vo = shipmentOrderService.queryById(id);
            List<? extends BaseOrderDetailVo> details = vo.getDetails();

            if (details == null || details.isEmpty()) {
                log.info("出库单 {} 无明细，无需删除终点容器", orderNo);
                return;
            }

            // 遍历出库明细，对每个终点库位调用 RCS 删除容器接口
            for (BaseOrderDetailVo detail : details) {
                String rackNo = detail.getContainerNo();
                String locationNo = detail.getTargetLocation();

                // 只处理有容器号和终点库位的明细
                if (StrUtil.isBlank(rackNo) || StrUtil.isBlank(locationNo)) {
                    continue;
                }

                RcsModels.EditRackLocationRequest request = new RcsModels.EditRackLocationRequest();
                request.setRackNo(rackNo);
                request.setType(2); // 2=删除
                request.setLocationNo(locationNo);

                try {
                    RcsModels.RcsResponse response = rcsClientService.editRackLocation(request);
                    if (response.getSuccess() != null && response.getSuccess()) {
                        log.info("出库单 {} 删除终点容器成功：rackNo={}, locationNo={}",
                            orderNo, rackNo, locationNo);
                    } else {
                        log.warn("出库单 {} 删除终点容器失败：rackNo={}, locationNo={}, 返回：{}",
                            orderNo, rackNo, locationNo,
                            response.getData() != null ? response.getData().getReturnInfo() : "未知错误");
                    }
                } catch (Exception e) {
                    log.error("出库单 {} 删除终点容器异常：rackNo={}, locationNo={}",
                        orderNo, rackNo, locationNo, e);
                }
            }
        } catch (Exception e) {
            log.error("删除终点容器异常，taskId：{}", taskId, e);
        }
    }

    /**
     * 根据 taskId 解析出对应明细的起点 sourceLocation。
     * <p>
     * taskId 格式：单据号-序号（如 RK07309690-001）。
     * 根据单据号前缀查询对应单据，按 needDispatch 规则遍历明细，
     * 找到序号匹配的明细，返回其 sourceLocation。
     *
     * @param taskId 任务号
     * @return 对应明细的 sourceLocation，解析失败返回 null
     */
    private String resolveSourceLocation(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return null;
        }
        int dashIndex = taskId.lastIndexOf('-');
        if (dashIndex < 0) {
            return null;
        }

        String orderNo = taskId.substring(0, dashIndex);
        int seq;
        try {
            seq = Integer.parseInt(taskId.substring(dashIndex + 1));
        } catch (NumberFormatException e) {
            return null;
        }

        try {
            List<? extends BaseOrderDetailVo> details;
            if (orderNo.startsWith("RK")) {
                Long id = receiptOrderService.queryIdByOrderNo(orderNo);
                if (id == null) return null;
                details = receiptOrderService.queryById(id).getDetails();
            } else if (orderNo.startsWith("CK")) {
                Long id = shipmentOrderService.queryIdByOrderNo(orderNo);
                if (id == null) return null;
                details = shipmentOrderService.queryById(id).getDetails();
            } else if (orderNo.startsWith("YK")) {
                MovementOrderVo vo = findMovementOrderByNo(orderNo);
                if (vo == null) return null;
                details = vo.getDetails();
            } else {
                return null;
            }

            if (details == null || details.isEmpty()) {
                return null;
            }

            int currentSeq = 1;
            for (BaseOrderDetailVo detail : details) {
                if (RcsTaskHelper.needDispatch(toBo(detail))) {
                    if (currentSeq == seq) {
                        return detail.getSourceLocation();
                    }
                    currentSeq++;
                }
            }
        } catch (Exception e) {
            log.error("解析 sourceLocation 异常，taskId：{}", taskId, e);
        }
        return null;
    }

    /**
     * 根据 taskId 解析出对应明细的终点 targetLocation。
     * <p>
     * taskId 格式：单据号-序号（如 CK07309690-001）。
     * 根据单据号前缀查询对应单据，按 needDispatch 规则遍历明细，
     * 找到序号匹配的明细，返回其 targetLocation。
     *
     * @param taskId 任务号
     * @return 对应明细的 targetLocation，解析失败返回 null
     */
    private String resolveTargetLocation(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return null;
        }
        int dashIndex = taskId.lastIndexOf('-');
        if (dashIndex < 0) {
            return null;
        }

        String orderNo = taskId.substring(0, dashIndex);
        int seq;
        try {
            seq = Integer.parseInt(taskId.substring(dashIndex + 1));
        } catch (NumberFormatException e) {
            return null;
        }

        try {
            List<? extends BaseOrderDetailVo> details;
            if (orderNo.startsWith("RK")) {
                Long id = receiptOrderService.queryIdByOrderNo(orderNo);
                if (id == null) return null;
                details = receiptOrderService.queryById(id).getDetails();
            } else if (orderNo.startsWith("CK")) {
                Long id = shipmentOrderService.queryIdByOrderNo(orderNo);
                if (id == null) return null;
                details = shipmentOrderService.queryById(id).getDetails();
            } else if (orderNo.startsWith("YK")) {
                MovementOrderVo vo = findMovementOrderByNo(orderNo);
                if (vo == null) return null;
                details = vo.getDetails();
            } else {
                return null;
            }

            if (details == null || details.isEmpty()) {
                return null;
            }

            int currentSeq = 1;
            for (BaseOrderDetailVo detail : details) {
                if (RcsTaskHelper.needDispatch(toBo(detail))) {
                    if (currentSeq == seq) {
                        return detail.getTargetLocation();
                    }
                    currentSeq++;
                }
            }
        } catch (Exception e) {
            log.error("解析 targetLocation 异常，taskId：{}", taskId, e);
        }
        return null;
    }

    /**
     * 查找移库单（移库单服务没有 queryIdByOrderNo，通过列表查询实现）。
     */
    private MovementOrderVo findMovementOrderByNo(String orderNo) {
        var bo = new com.ruoyi.wms.domain.bo.MovementOrderBo();
        bo.setOrderNo(orderNo);
        List<MovementOrderVo> list = movementOrderService.queryList(bo);
        if (list.isEmpty()) {
            return null;
        }
        // queryList 不返回明细，需要通过 queryById 获取完整数据
        return movementOrderService.queryById(list.get(0).getId());
    }

    /**
     * 将 VO 转换为 BO（仅复制 RCS 需要的字段）。
     */
    private BaseOrderDetailBo toBo(BaseOrderDetailVo vo) {
        BaseOrderDetailBo bo = new BaseOrderDetailBo();
        bo.setContainerNo(vo.getContainerNo());
        bo.setSourceLocation(vo.getSourceLocation());
        bo.setTargetLocation(vo.getTargetLocation());
        return bo;
    }

    /**
     * 保存安全信号日志
     */
    private void saveAgvSignLog(RcsModels.AgvSignRequest request) {
        try {
            AgvLog agvLog = new AgvLog();
            agvLog.setCallbackType("AgvSign");
            agvLog.setTaskId(request.getTaskId());
            agvLog.setActionType(request.getActionType());
            agvLog.setLocation(request.getLocation());
            agvLog.setBarCode(request.getBarCode());
            agvLog.setReportTime(request.getReportTime());
            agvLog.setRequestBody(JSONUtil.toJsonStr(request));
            agvLogService.saveLog(agvLog);
        } catch (Exception e) {
            log.error("保存 AGV 安全信号日志失败", e);
        }
    }
}

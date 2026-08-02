package com.ruoyi.wms.rcs;

import lombok.Data;

import java.util.List;

/**
 * RCS 标准接口请求与响应模型。
 */
public final class RcsModels {

    private RcsModels() {
    }

    @Data
    public static class TaskReceiveRequest {
        private String groupId;
        private Long msgTime;
        private List<TaskItem> tasks;
    }

    @Data
    public static class TaskItem {
        /**
         * 任务编号，必须唯一。
         */
        private String taskId;

        /**
         * 任务类型：0 入库，1 出库，2 移库。
         */
        private Integer taskType;

        /**
         * 优先级，范围 1~1000，数字越大优先级越高。
         */
        private Integer priorityCode;

        /**
         * 任务库区：cy、rgv、nz、fs、fb、fr、fork 等。
         */
        private String district;

        /**
         * 容器类型：1 托盘，2 周转箱，3 纸箱原箱，4 LL面料架，5 空托盘组，6 料架。
         */
        private Integer containerType;

        /**
         * 容器编号。
         */
        private String barCode;

        /**
         * 起点点位，使用区域时传空字符串。
         */
        private String startNode;

        /**
         * 起点区域，使用点位时传 0。
         */
        private Integer fromArea;

        /**
         * 终点点位，使用区域时传空字符串。
         */
        private String endNode;

        /**
         * 终点区域，使用点位时传 0。
         */
        private Integer toArea;

        /**
         * 侧叉拆叠盘指定层数，任务类型入库时可用。
         */
        private Integer layer;

        /**
         * 叠货时必填，目标货位高度。
         */
        private Double height;

        /**
         * 肥波任务必填：料架方向 A 或 B。
         */
        private String rackDirection;
    }

    @Data
    public static class ModifyPriorityRequest {
        private String taskId;
        private Long reportTime;
        private String district;
        private Integer priorityCode;
    }

    @Data
    public static class CancelTaskRequest {
        private String taskId;
        private Long reportTime;
    }

    /**
     * 按订单批量撤销 RCS 任务的请求。
     */
    @Data
    public static class CancelTaskByOrderRequest {
        private String orderNo;
        private Integer taskType;
        private List<CancelTaskDetailInfo> details;
    }

    @Data
    public static class CancelTaskDetailInfo {
        private String containerNo;
        private String sourceLocation;
        private String targetLocation;
    }

    @Data
    public static class TaskStatusReport {
        private String taskId;
        private String siteCode;
        private String rackNo;
        private Integer vid;
        private Integer height;
        private Long reportTime;
        private Integer taskStatus;
        private String returnInfo;
        private String errCode;
        private Integer errType;
    }

    @Data
    public static class AgvSignRequest {
        private Long reportTime;
        /**
         * 1 放货，2 取货。
         */
        private String actionType;
        private String location;
        private String taskId;
        private String barCode;
    }

    @Data
    public static class RcsResponse {
        private Integer code;
        private Long reportTime;
        private Boolean success;
        private RcsResponseData data;
    }

    @Data
    public static class RcsResponseData {
        private String groupId;
        private String taskId;
        private Integer returnStatus;
        private String returnInfo;
    }

    @Data
    public static class CallbackResponse {
        private Integer returnStatus;
        private String returnInfo;
        private Long msgTime;

        public static CallbackResponse ok() {
            CallbackResponse response = new CallbackResponse();
            response.setReturnStatus(0);
            response.setReturnInfo("");
            response.setMsgTime(System.currentTimeMillis());
            return response;
        }
    }

    @Data
    public static class AgvSignResponse {
        private Integer code;
        private Long reportTime;
        private Boolean success;
        private AgvSignData data;

        public static AgvSignResponse allow() {
            AgvSignResponse response = new AgvSignResponse();
            response.setCode(0);
            response.setReportTime(System.currentTimeMillis());
            response.setSuccess(true);
            AgvSignData data = new AgvSignData();
            data.setCanPut(1);
            response.setData(data);
            return response;
        }
    }

    @Data
    public static class AgvSignData {
        /**
         * 0 不允许，1 允许。
         */
        private Integer canPut;
    }

    /**
     * 容器位置修改请求（editRackLocation）。
     * <p>
     * 用于同步容器位置变更到 RCS 系统，适用 CTU、侧叉等车型。
     * type: 1=增加, 2=删除, 3=修改
     */
    @Data
    public static class EditRackLocationRequest {
        /**
         * 请求时间戳（毫秒）。
         */
        private Long timestamp;

        /**
         * 容器编码。
         */
        private String rackNo;

        /**
         * 1:增加，2:删除，3:修改。
         */
        private Integer type;

        /**
         * 增改容器新位置，删容器旧位置。
         */
        private String locationNo;

        /**
         * 容器原有库区。
         */
        private String district;
    }
}

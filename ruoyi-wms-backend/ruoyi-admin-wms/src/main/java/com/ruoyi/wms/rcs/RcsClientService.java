package com.ruoyi.wms.rcs;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RCS 网关 HTTP 客户端。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RcsClientService {

    private final RcsProperties properties;

    public RcsModels.RcsResponse taskReceive(RcsModels.TaskReceiveRequest request) {
        if (request.getMsgTime() == null) {
            request.setMsgTime(System.currentTimeMillis());
        }
        fillTaskDefaults(request);
        return post("/api/v1/fromWms/taskReceive", request);
    }

    public RcsModels.RcsResponse modifyPriority(RcsModels.ModifyPriorityRequest request) {
        if (request.getReportTime() == null) {
            request.setReportTime(System.currentTimeMillis());
        }
        if (StrUtil.isBlank(request.getDistrict())) {
            request.setDistrict(properties.getDefaultDistrict());
        }
        return post("/api/v1/fromWms/modTask", request);
    }

    public RcsModels.RcsResponse cancelTask(RcsModels.CancelTaskRequest request) {
        if (request.getReportTime() == null) {
            request.setReportTime(System.currentTimeMillis());
        }
        return post("/api/v1/fromWms/cancelTask", request);
    }

    /**
     * 修改容器位置（增/删/改）。
     * <p>
     * RCS 系统内的容器由于未通过 RCS 系统进行搬运而发生了变更，
     * 上位系统需要同步给 RCS 系统。
     *
     * @param request 容器位置修改请求
     * @return RCS 响应
     */
    public RcsModels.RcsResponse editRackLocation(RcsModels.EditRackLocationRequest request) {
        if (request.getTimestamp() == null) {
            request.setTimestamp(System.currentTimeMillis());
        }
        if (StrUtil.isBlank(request.getDistrict())) {
            request.setDistrict(properties.getDefaultDistrict());
        }
        return post("/api/v1/rcsTask/nz/editRackLocation", request);
    }

    private void fillTaskDefaults(RcsModels.TaskReceiveRequest request) {
        if (request.getTasks() == null) {
            return;
        }
        for (RcsModels.TaskItem task : request.getTasks()) {
            if (StrUtil.isBlank(task.getDistrict())) {
                task.setDistrict(properties.getDefaultDistrict());
            }
            if (task.getContainerType() == null) {
                task.setContainerType(properties.getDefaultContainerType());
            }
            if (StrUtil.isBlank(task.getStartNode())) {
                task.setStartNode("");
            }
            if (StrUtil.isBlank(task.getEndNode())) {
                task.setEndNode("");
            }
            if (task.getFromArea() == null) {
                task.setFromArea(0);
            }
            if (task.getToArea() == null) {
                task.setToArea(0);
            }
        }
    }

    private RcsModels.RcsResponse post(String path, Object requestBody) {
        String url = buildUrl(path);
        String body = JSONUtil.toJsonStr(requestBody);
        try {
            log.info("调用 RCS 接口 url={}, body={}", url, body);
            HttpResponse response = HttpRequest.post(url)
                .contentType(ContentType.JSON.toString())
                .charset("UTF-8")
                .timeout(properties.getTimeout())
                .body(body)
                .execute();
            String responseBody = response.body();
            log.info("RCS 接口响应 status={}, body={}", response.getStatus(), responseBody);
            if (!response.isOk()) {
                throw new ServiceException("RCS 接口调用失败，HTTP 状态码：" + response.getStatus());
            }
            return JSONUtil.toBean(responseBody, RcsModels.RcsResponse.class);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("RCS 接口调用异常：" + e.getMessage());
        }
    }

    private String buildUrl(String path) {
        String baseUrl = properties.getBaseUrl();
        if (StrUtil.isBlank(baseUrl)) {
            throw new ServiceException("RCS 地址未配置，请配置 rcs.base-url");
        }
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "http://" + baseUrl;
        }
        return StrUtil.removeSuffix(baseUrl, "/") + path;
    }
}

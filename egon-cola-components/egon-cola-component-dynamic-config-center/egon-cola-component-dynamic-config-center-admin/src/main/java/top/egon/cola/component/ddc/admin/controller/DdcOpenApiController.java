package top.egon.cola.component.ddc.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.service.DdcConfigService;
import top.egon.cola.component.ddc.admin.service.DdcInstanceAdminService;
import top.egon.cola.component.ddc.admin.service.DdcPublishService;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcDefaultReportRequest;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/openapi")
public class DdcOpenApiController {

    private final DdcInstanceAdminService instanceAdminService;

    private final DdcConfigService configService;

    private final DdcPublishService publishService;

    public DdcOpenApiController(DdcInstanceAdminService instanceAdminService,
                                DdcConfigService configService,
                                DdcPublishService publishService) {
        this.instanceAdminService = instanceAdminService;
        this.configService = configService;
        this.publishService = publishService;
    }

    @PostMapping("/instances/register")
    public ResultRecord<DdcLeaseSession> register(@RequestBody DdcInstanceRegisterRequest request) {
        return ResultRecord.success(instanceAdminService.register(request));
    }

    @PostMapping("/instances/heartbeat")
    public ResultRecord<DdcLeaseOperationResult> heartbeat(@RequestBody DdcHeartbeatRequest request) {
        return ResultRecord.success(instanceAdminService.heartbeat(request));
    }

    @PostMapping("/instances/offline")
    public ResultRecord<DdcLeaseOperationResult> offline(@RequestBody DdcHeartbeatRequest request) {
        return ResultRecord.success(instanceAdminService.offline(request));
    }

    @GetMapping("/configs/pull")
    public ResultRecord<List<DdcConfigValue>> pull(@RequestParam("appCode") String appCode,
                                             @RequestParam("env") String env,
                                             @RequestParam("namespace") String namespace) {
        return ResultRecord.success(configService.pull(appCode, env, namespace));
    }

    @GetMapping("/configs/{key}")
    public ResultRecord<DdcConfigValue> value(@RequestParam("appCode") String appCode,
                                        @RequestParam("env") String env,
                                        @RequestParam("namespace") String namespace,
                                        @PathVariable("key") String key) {
        return ResultRecord.success(configService.value(appCode, env, namespace, key));
    }

    @PostMapping("/publish/ack")
    public ResultRecord<?> ack(@RequestBody DdcAckRequest request) {
        return ResultRecord.success(publishService.ack(request));
    }

    @PostMapping("/defaults/report")
    public ResultRecord<Void> reportDefaults(@RequestBody DdcDefaultReportRequest request) {
        configService.reportDefaults(request);
        return ResultRecord.success(null);
    }
}

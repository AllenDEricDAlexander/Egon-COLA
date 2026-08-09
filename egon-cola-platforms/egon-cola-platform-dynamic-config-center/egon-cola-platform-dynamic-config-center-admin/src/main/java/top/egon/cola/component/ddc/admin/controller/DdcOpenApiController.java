package top.egon.cola.component.ddc.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.service.config.DdcConfigService;
import top.egon.cola.component.ddc.admin.service.lease.DdcInstanceAdminService;
import top.egon.cola.component.ddc.admin.service.publish.DdcPublishService;
import top.egon.cola.component.ddc.model.config.DdcAckRequest;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.config.DdcConfigValue;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;

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
    public ResultRecord<List<DdcConfigValue>> pull(
            @RequestParam("bizCode") String bizCode,
            @RequestParam("env") String env,
            @RequestParam("appCode") String appCode) {
        return ResultRecord.success(configService.pull(bizCode, env, appCode));
    }

    @PostMapping("/publish/ack")
    public ResultRecord<?> ack(@RequestBody DdcAckRequest request) {
        return ResultRecord.success(publishService.ack(request));
    }
}

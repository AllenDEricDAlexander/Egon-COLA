package top.egon.cola.component.ddc.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.result.Result;
import top.egon.cola.component.ddc.admin.service.DdcConfigService;
import top.egon.cola.component.ddc.admin.service.DdcInstanceAdminService;
import top.egon.cola.component.ddc.admin.service.DdcPublishService;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcDefaultReportRequest;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;

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
    public Result<Void> register(@RequestBody DdcInstanceRegisterRequest request) {
        instanceAdminService.register(request);
        return Result.success();
    }

    @PostMapping("/instances/heartbeat")
    public Result<Void> heartbeat(@RequestBody DdcHeartbeatRequest request) {
        instanceAdminService.heartbeat(request);
        return Result.success();
    }

    @PostMapping("/instances/offline")
    public Result<Void> offline(@RequestBody DdcHeartbeatRequest request) {
        instanceAdminService.offline(request);
        return Result.success();
    }

    @GetMapping("/configs/pull")
    public Result<List<DdcConfigValue>> pull(@RequestParam("appCode") String appCode,
                                             @RequestParam("env") String env,
                                             @RequestParam("namespace") String namespace) {
        return Result.success(configService.pull(appCode, env, namespace));
    }

    @GetMapping("/configs/{key}")
    public Result<DdcConfigValue> value(@RequestParam("appCode") String appCode,
                                        @RequestParam("env") String env,
                                        @RequestParam("namespace") String namespace,
                                        @PathVariable("key") String key) {
        return Result.success(configService.value(appCode, env, namespace, key));
    }

    @PostMapping("/publish/ack")
    public Result<?> ack(@RequestBody DdcAckRequest request) {
        return Result.success(publishService.ack(request));
    }

    @PostMapping("/defaults/report")
    public Result<Void> reportDefaults(@RequestBody DdcDefaultReportRequest request) {
        configService.reportDefaults(request);
        return Result.success();
    }
}

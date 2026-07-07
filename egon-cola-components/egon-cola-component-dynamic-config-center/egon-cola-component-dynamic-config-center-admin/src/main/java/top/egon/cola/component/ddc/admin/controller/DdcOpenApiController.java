package top.egon.cola.component.ddc.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.result.dto.ResultDto;
import top.egon.cola.component.common.result.factory.ResultDtos;
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
    public ResultDto<Void> register(@RequestBody DdcInstanceRegisterRequest request) {
        instanceAdminService.register(request);
        return ResultDtos.success();
    }

    @PostMapping("/instances/heartbeat")
    public ResultDto<Void> heartbeat(@RequestBody DdcHeartbeatRequest request) {
        instanceAdminService.heartbeat(request);
        return ResultDtos.success();
    }

    @PostMapping("/instances/offline")
    public ResultDto<Void> offline(@RequestBody DdcHeartbeatRequest request) {
        instanceAdminService.offline(request);
        return ResultDtos.success();
    }

    @GetMapping("/configs/pull")
    public ResultDto<List<DdcConfigValue>> pull(@RequestParam("appCode") String appCode,
                                             @RequestParam("env") String env,
                                             @RequestParam("namespace") String namespace) {
        return ResultDtos.success(configService.pull(appCode, env, namespace));
    }

    @GetMapping("/configs/{key}")
    public ResultDto<DdcConfigValue> value(@RequestParam("appCode") String appCode,
                                        @RequestParam("env") String env,
                                        @RequestParam("namespace") String namespace,
                                        @PathVariable("key") String key) {
        return ResultDtos.success(configService.value(appCode, env, namespace, key));
    }

    @PostMapping("/publish/ack")
    public ResultDto<?> ack(@RequestBody DdcAckRequest request) {
        return ResultDtos.success(publishService.ack(request));
    }

    @PostMapping("/defaults/report")
    public ResultDto<Void> reportDefaults(@RequestBody DdcDefaultReportRequest request) {
        configService.reportDefaults(request);
        return ResultDtos.success();
    }
}

package top.egon.cola.component.ddc.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.service.config.DdcConfigFacade;
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

    private final DdcConfigFacade facade;

    public DdcOpenApiController(DdcConfigFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/instances/register")
    public ResultRecord<DdcLeaseSession> register(@RequestBody DdcInstanceRegisterRequest request) {
        return ResultRecord.success(facade.register(request));
    }

    @PostMapping("/instances/heartbeat")
    public ResultRecord<DdcLeaseOperationResult> heartbeat(@RequestBody DdcHeartbeatRequest request) {
        return ResultRecord.success(facade.heartbeat(request));
    }

    @PostMapping("/instances/offline")
    public ResultRecord<DdcLeaseOperationResult> offline(@RequestBody DdcHeartbeatRequest request) {
        return ResultRecord.success(facade.offline(request));
    }

    @GetMapping("/configs/pull")
    public ResultRecord<List<DdcConfigValue>> pull(
            @RequestParam("bizCode") String bizCode,
            @RequestParam("env") String env,
            @RequestParam("appCode") String appCode) {
        return ResultRecord.success(facade.pull(bizCode, env, appCode));
    }

    @PostMapping("/publish/ack")
    public ResultRecord<?> ack(@RequestBody DdcAckRequest request) {
        return ResultRecord.success(facade.ack(request));
    }
}

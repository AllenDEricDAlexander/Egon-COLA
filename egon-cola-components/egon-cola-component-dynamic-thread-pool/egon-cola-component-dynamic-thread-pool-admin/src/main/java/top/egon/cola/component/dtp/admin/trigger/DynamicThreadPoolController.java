package top.egon.cola.component.dtp.admin.trigger;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RList;
import org.redisson.api.RSet;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.registry.model.DtpAuditEvent;
import top.egon.cola.component.dtp.registry.model.DtpConfigChangeMessage;
import top.egon.cola.component.dtp.registry.model.DtpRedisKeys;
import top.egon.cola.component.dtp.admin.trigger.model.ResizeExecutorRequest;
import top.egon.cola.component.dtp.admin.trigger.model.VirtualLimitRequest;
import top.egon.cola.component.dtp.admin.types.Response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @ClassName: DynamicThreadPoolController
 * @description: 动态线程池管理接口
 * @author: 有罗敷的马同学
 * @datetime: 2025Year-04Month-14Day-下午7:34
 * @Version: 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dtp")
public class DynamicThreadPoolController {

    @Resource
    public RedissonClient redissonClient;

    @GetMapping("/apps")
    public Response<Set<String>> queryApps() {
        try {
            RSet<String> apps = redissonClient.getSet(DtpRedisKeys.apps());
            return Response.success(apps.readAll());
        } catch (Exception e) {
            log.error("查询动态线程池应用列表异常", e);
            return Response.fail(Response.Code.UN_ERROR.getInfo());
        }
    }

    @GetMapping("/apps/{appName}/instances")
    public Response<Set<String>> queryInstances(@PathVariable String appName) {
        try {
            RSet<String> instances = redissonClient.getSet(DtpRedisKeys.instances(appName));
            return Response.success(instances.readAll());
        } catch (Exception e) {
            log.error("查询动态线程池实例列表异常 {}", appName, e);
            return Response.fail(Response.Code.UN_ERROR.getInfo());
        }
    }

    @GetMapping("/apps/{appName}/instances/{instanceId}/executors")
    public Response<List<ExecutorSnapshot>> queryExecutors(@PathVariable String appName, @PathVariable String instanceId) {
        try {
            List<ExecutorSnapshot> snapshots = new ArrayList<>();
            RKeys keys = redissonClient.getKeys();
            for (String key : keys.getKeysByPattern(DtpRedisKeys.snapshot(appName, instanceId, "*"))) {
                RBucket<ExecutorSnapshot> bucket = redissonClient.getBucket(key);
                ExecutorSnapshot snapshot = bucket.get();
                if (snapshot != null) {
                    snapshots.add(snapshot);
                }
            }
            return Response.success(snapshots);
        } catch (Exception e) {
            log.error("查询动态线程池执行器列表异常 {} {}", appName, instanceId, e);
            return Response.fail(Response.Code.UN_ERROR.getInfo());
        }
    }

    @GetMapping("/apps/{appName}/instances/{instanceId}/executors/{executorName}")
    public Response<ExecutorSnapshot> queryExecutor(@PathVariable String appName,
                                                    @PathVariable String instanceId,
                                                    @PathVariable String executorName) {
        try {
            RBucket<ExecutorSnapshot> bucket = redissonClient.getBucket(DtpRedisKeys.snapshot(appName, instanceId, executorName));
            return Response.success(bucket.get());
        } catch (Exception e) {
            log.error("查询动态线程池执行器快照异常 {} {} {}", appName, instanceId, executorName, e);
            return Response.fail(Response.Code.UN_ERROR.getInfo());
        }
    }

    @PostMapping("/apps/{appName}/instances/{instanceId}/executors/{executorName}/resize")
    public Response<Boolean> resizeExecutor(@PathVariable String appName,
                                            @PathVariable String instanceId,
                                            @PathVariable String executorName,
                                            @RequestBody ResizeExecutorRequest request) {
        String error = validateResizeRequest(request);
        if (error != null) {
            return Response.<Boolean>error(error).setDataValue(false);
        }
        try {
            ExecutorKind executorKind = request.getExecutorKind();
            DtpConfigChangeMessage message = buildConfigChangeMessage(appName, instanceId, executorName,
                    executorKind, request.getOperator());
            ExecutorUpdateCommand command = buildBaseCommand(appName, instanceId, executorName,
                    executorKind, request.getOperator());
            command.setCorePoolSize(request.getCorePoolSize());
            command.setMaximumPoolSize(request.getMaximumPoolSize());
            command.setKeepAliveSeconds(request.getKeepAliveSeconds());
            command.setAllowCoreThreadTimeOut(request.getAllowCoreThreadTimeOut());
            message.setPayload(command);
            redissonClient.getTopic(DtpRedisKeys.changeTopic(appName)).publish(message);
            return Response.success(true);
        } catch (Exception e) {
            log.error("发布动态线程池容量调整消息异常 {} {} {} {}", appName, instanceId, executorName, request, e);
            return Response.<Boolean>fail(Response.Code.UN_ERROR.getInfo()).setDataValue(false);
        }
    }

    @PostMapping("/apps/{appName}/instances/{instanceId}/executors/{executorName}/virtual-limit")
    public Response<Boolean> updateVirtualLimit(@PathVariable String appName,
                                                @PathVariable String instanceId,
                                                @PathVariable String executorName,
                                                @RequestBody VirtualLimitRequest request) {
        String error = validateVirtualLimitRequest(request);
        if (error != null) {
            return Response.<Boolean>error(error).setDataValue(false);
        }
        try {
            DtpConfigChangeMessage message = buildConfigChangeMessage(appName, instanceId, executorName,
                    ExecutorKind.VIRTUAL_THREAD_PER_TASK, request.getOperator());
            ExecutorUpdateCommand command = buildBaseCommand(appName, instanceId, executorName,
                    ExecutorKind.VIRTUAL_THREAD_PER_TASK, request.getOperator());
            command.setConcurrencyLimit(request.getConcurrencyLimit());
            message.setPayload(command);
            redissonClient.getTopic(DtpRedisKeys.changeTopic(appName)).publish(message);
            return Response.success(true);
        } catch (Exception e) {
            log.error("发布动态线程池虚拟线程并发限制调整消息异常 {} {} {} {}", appName, instanceId, executorName, request, e);
            return Response.<Boolean>fail(Response.Code.UN_ERROR.getInfo()).setDataValue(false);
        }
    }

    @GetMapping("/events")
    public Response<List<DtpAuditEvent>> queryEvents(@RequestParam String appName, @RequestParam String date) {
        try {
            RList<DtpAuditEvent> events = redissonClient.getList(DtpRedisKeys.event(appName, date));
            return Response.success(events.readAll());
        } catch (Exception e) {
            log.error("查询动态线程池审计事件异常 {} {}", appName, date, e);
            return Response.fail(Response.Code.UN_ERROR.getInfo());
        }
    }

    private String validateResizeRequest(ResizeExecutorRequest request) {
        if (request == null) {
            return "resize request must not be null";
        }
        if (!isResizableExecutorKind(request.getExecutorKind())) {
            return "executorKind must be PLATFORM_THREAD_POOL or SPRING_THREAD_POOL_TASK_EXECUTOR";
        }
        if (request.getCorePoolSize() == null || request.getCorePoolSize() <= 0) {
            return "corePoolSize must be positive";
        }
        if (request.getMaximumPoolSize() == null || request.getMaximumPoolSize() <= 0) {
            return "maximumPoolSize must be positive";
        }
        if (request.getCorePoolSize() > request.getMaximumPoolSize()) {
            return "corePoolSize must be less than or equal to maximumPoolSize";
        }
        if (Boolean.TRUE.equals(request.getAllowCoreThreadTimeOut())
                && (request.getKeepAliveSeconds() == null || request.getKeepAliveSeconds() <= 0)) {
            return "keepAliveSeconds must be positive when allowCoreThreadTimeOut is true";
        }
        return null;
    }

    private boolean isResizableExecutorKind(ExecutorKind executorKind) {
        return executorKind == ExecutorKind.PLATFORM_THREAD_POOL
                || executorKind == ExecutorKind.SPRING_THREAD_POOL_TASK_EXECUTOR;
    }

    private String validateVirtualLimitRequest(VirtualLimitRequest request) {
        if (request == null) {
            return "virtual limit request must not be null";
        }
        if (request.getConcurrencyLimit() == null || request.getConcurrencyLimit() <= 0) {
            return "concurrencyLimit must be positive";
        }
        return null;
    }

    private DtpConfigChangeMessage buildConfigChangeMessage(String appName, String instanceId, String executorName,
                                                            ExecutorKind executorKind, String operator) {
        DtpConfigChangeMessage message = new DtpConfigChangeMessage();
        message.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        message.setTraceId(MDC.get("traceId"));
        message.setRequestId(MDC.get("requestId"));
        message.setAppName(appName);
        message.setInstanceId(instanceId);
        message.setExecutorName(executorName);
        message.setExecutorKind(executorKind);
        message.setOperator(operator);
        message.setTimestamp(Instant.now());
        return message;
    }

    private ExecutorUpdateCommand buildBaseCommand(String appName, String instanceId, String executorName,
                                                   ExecutorKind executorKind, String operator) {
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setAppName(appName);
        command.setInstanceId(instanceId);
        command.setExecutorName(executorName);
        command.setExecutorKind(executorKind);
        command.setTraceId(MDC.get("traceId"));
        command.setRequestId(MDC.get("requestId"));
        command.setOperator(operator);
        return command;
    }

}

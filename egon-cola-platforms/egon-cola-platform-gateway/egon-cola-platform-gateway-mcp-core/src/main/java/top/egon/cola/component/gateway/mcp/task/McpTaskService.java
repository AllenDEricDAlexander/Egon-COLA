package top.egon.cola.component.gateway.mcp.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.security.McpSecurityDigests;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Owns task IDs, owner isolation and all state-machine transitions.
 * 补充说明 / Supplementary summary: {@code McpTaskService} 是服务组件，位于当前 Gateway 模块的相关包中，负责MCP任务服务相关的职责与边界。
 * English supplement: {@code McpTaskService} is a mcp task service service in the current Gateway module; it owns the mcp task service-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpTaskService {

    /**
     * 中文说明：保存 存储 对应的状态、依赖或配置值；字段类型为 {@code McpTaskStore}，由 {@code McpTaskService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by store; its type is {@code McpTaskStore}, and {@code McpTaskService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpTaskStore store;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code McpTaskService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code McpTaskService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code McpTaskService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code McpTaskService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 租约Duration 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpTaskService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by lease duration; its type is {@code Duration}, and {@code McpTaskService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration leaseDuration;

    /**
     * 中文说明：保存 random 对应的状态、依赖或配置值；字段类型为 {@code SecureRandom}，由 {@code McpTaskService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by random; its type is {@code SecureRandom}, and {@code McpTaskService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final SecureRandom random = new SecureRandom();

    /**
     * 中文说明：保存 states 对应的状态、依赖或配置值；字段类型为 {@code McpTaskStateMachine}，由 {@code McpTaskService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by states; its type is {@code McpTaskStateMachine}, and {@code McpTaskService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpTaskStateMachine states = new McpTaskStateMachine();

    /**
     * 中文说明：创建 {@code McpTaskService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpTaskService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param store 参数 存储；parameter store。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param clock 参数 clock；parameter clock。
     * @param leaseDuration 参数 租约Duration；parameter lease duration。
     */
    public McpTaskService(
            McpTaskStore store,
            ObjectMapper objectMapper,
            Clock clock,
            Duration leaseDuration) {
        this.store = Objects.requireNonNull(store, "store");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
        this.leaseDuration = positive(leaseDuration, "leaseDuration");
    }

    /**
     * 中文说明：执行 create 操作；该方法是 {@code McpTaskService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code McpTaskService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param owner 参数 owner；parameter owner。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
    public Publisher<McpTask> create(
            CreateRequest request,
            Owner owner) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(owner, "owner");
        validatePayload(request.inputPayload());
        Instant now = clock.instant();
        McpTask task = new McpTask(
                taskId(),
                owner.fingerprint(),
                owner.subjectId(),
                owner.tenantId(),
                owner.clientId(),
                request.serverCode(),
                request.toolName(),
                request.requestDigest(),
                McpTask.State.WORKING,
                request.inputPayload(),
                null,
                null,
                null,
                null,
                now.plus(request.executionTimeout()),
                now.plus(request.resultTtl()),
                0,
                request.maxAttempts(),
                0L,
                now,
                now
        );
        return Mono.from(store.create(task)).thenReturn(task);
    }

    /**
     * 中文说明：执行 get 操作；该方法是 {@code McpTaskService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get operation; this method is the invocation entry point on {@code McpTaskService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.get(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param taskId 参数 任务Id；parameter task id。
     * @param owner 参数 owner；parameter owner。
     * @return 返回 get 的处理结果；returns the result of the operation.
     */
    public Publisher<McpTask> get(String taskId, Owner owner) {
        return owned(taskId, owner).flatMap(task -> {
            if (task.terminal() && !task.expiresAt().isAfter(clock.instant())) {
                return Mono.error(notFound());
            }
            return Mono.just(task);
        });
    }

    /**
     * 中文说明：执行 provideInput 操作；该方法是 {@code McpTaskService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the provide input operation; this method is the invocation entry point on {@code McpTaskService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.provideInput(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param taskId 参数 任务Id；parameter task id。
     * @param inputRequestKey 参数 input请求键；parameter input request key。
     * @param input 参数 input；parameter input。
     * @param owner 参数 owner；parameter owner。
     * @return 返回 provideInput 的处理结果；returns the result of the operation.
     */
    public Publisher<McpTask> provideInput(
            String taskId,
            String inputRequestKey,
            Map<String, Object> input,
            Owner owner) {
        return owned(taskId, owner).flatMap(task -> {
            McpTask.State target = states.transition(
                    task.state(),
                    McpTaskStateMachine.Event.PROVIDE_INPUT
            );
            Object expected = task.inputPayload().get("inputRequestKey");
            if (!(expected instanceof String expectedKey)
                    || !expectedKey.equals(inputRequestKey)) {
                return Mono.error(invalid(
                        "MCP task input request key does not match"
                ));
            }
            LinkedHashMap<String, Object> payload = new LinkedHashMap<>(
                    task.inputPayload()
            );
            payload.put("inputResponseKey", inputRequestKey);
            payload.put("inputResponse", Map.copyOf(input));
            payload.remove("inputRequest");
            return transition(
                    task,
                    target,
                    null,
                    Map.copyOf(payload),
                    null,
                    null
            );
        });
    }

    /**
     * 中文说明：执行 cancel 操作；该方法是 {@code McpTaskService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the cancel operation; this method is the invocation entry point on {@code McpTaskService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.cancel(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param taskId 参数 任务Id；parameter task id。
     * @param owner 参数 owner；parameter owner。
     * @return 返回 cancel 的处理结果；returns the result of the operation.
     */
    public Publisher<McpTask> cancel(String taskId, Owner owner) {
        return owned(taskId, owner).flatMap(task -> {
            McpTask.State target = states.transition(
                    task.state(),
                    McpTaskStateMachine.Event.CANCEL
            );
            return Mono.from(store.cancel(
                            task.id(),
                            task.state(),
                            task.revision(),
                            clock.instant()
                    ))
                    .flatMap(updated -> updated
                            ? owned(task.id(), owner)
                            : Mono.error(conflict()));
        });
    }

    /**
     * 中文说明：执行 executeNext 操作；该方法是 {@code McpTaskService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the execute next operation; this method is the invocation entry point on {@code McpTaskService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.executeNext(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param workerOwner 参数 workerOwner；parameter worker owner。
     * @param executor 参数 executor；parameter executor。
     * @return 返回 executeNext 的处理结果；returns the result of the operation.
     */
    public Publisher<Void> executeNext(
            String workerOwner,
            McpTaskExecutor executor) {
        Instant now = clock.instant();
        return Mono.from(store.leaseNext(
                        workerOwner,
                        now,
                        now.plus(leaseDuration)
                ))
                .flatMap(task -> executeLeased(
                        task,
                        workerOwner,
                        executor
                ))
                .then();
    }

    /**
     * 中文说明：执行 executeLeased 操作；该方法是 {@code McpTaskService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the execute leased operation; this method is the invocation entry point on {@code McpTaskService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.executeLeased(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param task 参数 任务；parameter task。
     * @param workerOwner 参数 workerOwner；parameter worker owner。
     * @param executor 参数 executor；parameter executor。
     * @return 返回 executeLeased 的处理结果；returns the result of the operation.
     */
    private Mono<Void> executeLeased(
            McpTask task,
            String workerOwner,
            McpTaskExecutor executor) {
        Mono<McpTaskExecutor.Outcome> outcome = Mono.from(
                        executor.execute(task)
                )
                .onErrorReturn(McpTaskExecutor.Outcome.failed(Map.of(
                        "code", "MCP_TASK_EXECUTION_FAILED"
                )))
                .cache();
        Duration heartbeatInterval = leaseDuration.dividedBy(3L);
        Mono<Void> heartbeat = Flux.interval(heartbeatInterval)
                .concatMap(ignored -> {
                    Instant now = clock.instant();
                    return Mono.from(store.renewLease(
                                    task.id(),
                                    workerOwner,
                                    now,
                                    now.plus(leaseDuration)
                            ))
                            .flatMap(renewed -> renewed
                                    ? Mono.<Void>empty()
                                    : Mono.error(new IllegalStateException(
                                            "MCP task lease was lost"
                                    )));
                })
                .takeUntilOther(outcome)
                .then();
        return Mono.when(
                outcome.flatMap(value -> finish(
                        task,
                        workerOwner,
                        value
                )).then(),
                heartbeat
        );
    }

    /**
     * 中文说明：执行 cleanup 操作；该方法是 {@code McpTaskService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the cleanup operation; this method is the invocation entry point on {@code McpTaskService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.cleanup(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 cleanup 的处理结果；returns the result of the operation.
     */
    public Publisher<Integer> cleanup() {
        Instant now = clock.instant();
        return Mono.zip(
                Mono.from(store.failUnavailable(now)),
                Mono.from(store.deleteExpired(now))
        ).map(result -> result.getT1() + result.getT2());
    }

    /**
     * 中文说明：执行 finish 操作；该方法是 {@code McpTaskService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the finish operation; this method is the invocation entry point on {@code McpTaskService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.finish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param task 参数 任务；parameter task。
     * @param workerOwner 参数 workerOwner；parameter worker owner。
     * @param outcome 参数 outcome；parameter outcome。
     * @return 返回 finish 的处理结果；returns the result of the operation.
     */
    private Mono<McpTask> finish(
            McpTask task,
            String workerOwner,
            McpTaskExecutor.Outcome outcome) {
        return switch (outcome.type()) {
            case COMPLETED -> transition(
                    task,
                    states.transition(
                            task.state(),
                            McpTaskStateMachine.Event.COMPLETE
                    ),
                    workerOwner,
                    task.inputPayload(),
                    outcome.payload(),
                    null
            );
            case FAILED -> transition(
                    task,
                    states.transition(
                            task.state(),
                            McpTaskStateMachine.Event.FAIL
                    ),
                    workerOwner,
                    task.inputPayload(),
                    null,
                    outcome.payload()
            );
            case INPUT_REQUIRED -> {
                LinkedHashMap<String, Object> input = new LinkedHashMap<>(
                        task.inputPayload()
                );
                input.put("inputRequestKey", outcome.inputRequestKey());
                input.put("inputRequest", outcome.payload());
                input.remove("inputResponseKey");
                input.remove("inputResponse");
                yield transition(
                        task,
                        states.transition(
                                task.state(),
                                McpTaskStateMachine.Event.REQUEST_INPUT
                        ),
                        workerOwner,
                        Map.copyOf(input),
                        null,
                        null
                );
            }
        };
    }

    /**
     * 中文说明：执行 transition 操作；该方法是 {@code McpTaskService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the transition operation; this method is the invocation entry point on {@code McpTaskService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.transition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param task 参数 任务；parameter task。
     * @param target 参数 target；parameter target。
     * @param workerOwner 参数 workerOwner；parameter worker owner。
     * @param input 参数 input；parameter input。
     * @param result 参数 result；parameter result。
     * @param error 参数 error；parameter error。
     * @return 返回 transition 的处理结果；returns the result of the operation.
     */
    private Mono<McpTask> transition(
            McpTask task,
            McpTask.State target,
            String workerOwner,
            Map<String, Object> input,
            Map<String, Object> result,
            Map<String, Object> error) {
        return Mono.from(store.transition(new McpTaskStore.Transition(
                        task.id(),
                        task.state(),
                        target,
                        task.revision(),
                        workerOwner,
                        input,
                        result,
                        error,
                        clock.instant()
                )))
                .flatMap(updated -> updated
                        ? Mono.from(store.find(task.id()))
                        : Mono.error(conflict()));
    }

    /**
     * 中文说明：执行 owned 操作；该方法是 {@code McpTaskService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the owned operation; this method is the invocation entry point on {@code McpTaskService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.owned(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param taskId 参数 任务Id；parameter task id。
     * @param owner 参数 owner；parameter owner。
     * @return 返回 owned 的处理结果；returns the result of the operation.
     */
    private Mono<McpTask> owned(String taskId, Owner owner) {
        Objects.requireNonNull(owner, "owner");
        return Mono.from(store.find(required(taskId, "taskId")))
                .filter(task -> task.principalFingerprint().equals(
                                owner.fingerprint()
                        )
                        && task.subjectId().equals(owner.subjectId())
                        && task.tenantId().equals(owner.tenantId())
                        && task.clientId().equals(owner.clientId()))
                .switchIfEmpty(Mono.error(notFound()));
    }

    /**
     * 中文说明：执行 任务Id 操作；该方法是 {@code McpTaskService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the task id operation; this method is the invocation entry point on {@code McpTaskService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.taskId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 任务Id 的处理结果；returns the result of the operation.
     */
    private String taskId() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 中文说明：执行 validatePayload 操作；该方法是 {@code McpTaskService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate payload operation; this method is the invocation entry point on {@code McpTaskService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.validatePayload(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param payload 参数 payload；parameter payload。
     */
    private void validatePayload(Map<String, Object> payload) {
        try {
            if (objectMapper.writeValueAsBytes(payload).length > 1024 * 1024) {
                throw invalid("MCP task input payload is too large");
            }
        } catch (McpProtocolException failure) {
            throw failure;
        } catch (Exception failure) {
            throw invalid("MCP task input payload is invalid");
        }
    }

    /**
     * 中文说明：执行 notFound 操作；该方法是 {@code McpTaskService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the not found operation; this method is the invocation entry point on {@code McpTaskService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.notFound(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 notFound 的处理结果；returns the result of the operation.
     */
    private McpProtocolException notFound() {
        return new McpProtocolException(
                McpErrorCode.MCP_TASK_NOT_FOUND,
                "MCP task was not found"
        );
    }

    /**
     * 中文说明：执行 conflict 操作；该方法是 {@code McpTaskService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the conflict operation; this method is the invocation entry point on {@code McpTaskService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.conflict(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 conflict 的处理结果；returns the result of the operation.
     */
    private McpProtocolException conflict() {
        return invalid("MCP task changed concurrently");
    }

    /**
     * 中文说明：执行 invalid 操作；该方法是 {@code McpTaskService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invalid operation; this method is the invocation entry point on {@code McpTaskService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.invalid(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param message 参数 消息；parameter message。
     * @return 返回 invalid 的处理结果；returns the result of the operation.
     */
    private McpProtocolException invalid(String message) {
        return new McpProtocolException(
                McpErrorCode.MCP_INVALID_PARAMS,
                message
        );
    }

    /**
     * 中文说明：执行 positive 操作；该方法是 {@code McpTaskService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the positive operation; this method is the invocation entry point on {@code McpTaskService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.positive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 positive 的处理结果；returns the result of the operation.
     */
    private Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpTaskService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpTaskService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 中文说明：{@code Owner} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Owner相关的职责与边界。
     * English summary: {@code Owner} is an immutable data carrier in the current Gateway module; it owns the owner-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param subjectId 参数 subjectId；parameter subject id。
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     */
    public record Owner(
    /**
     * 中文说明：保存 subjectId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTaskService.Owner} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by subject id; its type is {@code String}, and {@code McpTaskService.Owner} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskService.Owner} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskService.Owner}; do not couple callers to its representation when the owning type exposes an API.
     */
    String subjectId,
    /**
     * 中文说明：保存 tenantId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTaskService.Owner} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by tenant id; its type is {@code String}, and {@code McpTaskService.Owner} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskService.Owner} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskService.Owner}; do not couple callers to its representation when the owning type exposes an API.
     */
    String tenantId,
    /**
     * 中文说明：保存 客户端Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTaskService.Owner} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by client id; its type is {@code String}, and {@code McpTaskService.Owner} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskService.Owner} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskService.Owner}; do not couple callers to its representation when the owning type exposes an API.
     */
    String clientId) {

        /**
         * 中文说明：创建 {@code McpTaskService.Owner} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpTaskService.Owner} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param subjectId 参数 subjectId；parameter subject id。
         * @param tenantId 参数 tenantId；parameter tenant id。
         * @param clientId 参数 客户端Id；parameter client id。
         */
        public Owner {
            subjectId = required(subjectId, "subjectId");
            tenantId = required(tenantId, "tenantId");
            clientId = required(clientId, "clientId");
        }

        /**
         * 中文说明：执行 fingerprint 操作；该方法是 {@code McpTaskService.Owner} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the fingerprint operation; this method is the invocation entry point on {@code McpTaskService.Owner} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.Owner.fingerprint(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 fingerprint 的处理结果；returns the result of the operation.
         */
        public String fingerprint() {
            return McpSecurityDigests.token(
                    subjectId + '\0' + tenantId + '\0' + clientId
            );
        }
    }

    /**
     * 中文说明：{@code CreateRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Create请求相关的职责与边界。
     * English summary: {@code CreateRequest} is an immutable data carrier in the current Gateway module; it owns the create request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param toolName 参数 工具Name；parameter tool name。
     * @param requestDigest 参数 请求Digest；parameter request digest。
     * @param inputPayload 参数 inputPayload；parameter input payload。
     * @param executionTimeout 参数 execution超时；parameter execution timeout。
     * @param resultTtl 参数 resultTtl；parameter result ttl。
     * @param maxAttempts 参数 maxAttempts；parameter max attempts。
     */
    public record CreateRequest(
            /**
             * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTaskService.CreateRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code McpTaskService.CreateRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskService.CreateRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskService.CreateRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverCode,
            /**
             * 中文说明：保存 工具Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTaskService.CreateRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tool name; its type is {@code String}, and {@code McpTaskService.CreateRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskService.CreateRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskService.CreateRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String toolName,
            /**
             * 中文说明：保存 请求Digest 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTaskService.CreateRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by request digest; its type is {@code String}, and {@code McpTaskService.CreateRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskService.CreateRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskService.CreateRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String requestDigest,
            /**
             * 中文说明：保存 inputPayload 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpTaskService.CreateRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by input payload; its type is {@code Map<String, Object>}, and {@code McpTaskService.CreateRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskService.CreateRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskService.CreateRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> inputPayload,
            /**
             * 中文说明：保存 execution超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpTaskService.CreateRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by execution timeout; its type is {@code Duration}, and {@code McpTaskService.CreateRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskService.CreateRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskService.CreateRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Duration executionTimeout,
            /**
             * 中文说明：保存 resultTtl 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpTaskService.CreateRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by result ttl; its type is {@code Duration}, and {@code McpTaskService.CreateRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskService.CreateRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskService.CreateRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Duration resultTtl,
            /**
             * 中文说明：保存 maxAttempts 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code McpTaskService.CreateRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by max attempts; its type is {@code int}, and {@code McpTaskService.CreateRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskService.CreateRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskService.CreateRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            int maxAttempts
    ) {

        /**
         * 中文说明：创建 {@code McpTaskService.CreateRequest} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpTaskService.CreateRequest} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param serverCode 参数 服务器Code；parameter server code。
         * @param toolName 参数 工具Name；parameter tool name。
         * @param requestDigest 参数 请求Digest；parameter request digest。
         * @param inputPayload 参数 inputPayload；parameter input payload。
         * @param executionTimeout 参数 execution超时；parameter execution timeout。
         * @param resultTtl 参数 resultTtl；parameter result ttl。
         * @param maxAttempts 参数 maxAttempts；parameter max attempts。
         */
        public CreateRequest {
            serverCode = required(serverCode, "serverCode");
            toolName = required(toolName, "toolName");
            requestDigest = required(requestDigest, "requestDigest");
            if (!requestDigest.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException(
                        "requestDigest must contain a SHA-256 digest"
                );
            }
            inputPayload = Map.copyOf(Objects.requireNonNull(
                    inputPayload,
                    "inputPayload"
            ));
            executionTimeout = requirePositive(
                    executionTimeout,
                    "executionTimeout"
            );
            resultTtl = requirePositive(resultTtl, "resultTtl");
            if (maxAttempts < 1 || maxAttempts > 100) {
                throw new IllegalArgumentException(
                        "maxAttempts must be between 1 and 100"
                );
            }
        }

        /**
         * 中文说明：执行 requirePositive 操作；该方法是 {@code McpTaskService.CreateRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the require positive operation; this method is the invocation entry point on {@code McpTaskService.CreateRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpTaskService.CreateRequest.requirePositive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param value 参数 值；parameter value。
         * @param field 参数 field；parameter field。
         * @return 返回 requirePositive 的处理结果；returns the result of the operation.
         */
        private static Duration requirePositive(
                Duration value,
                String field) {
            Objects.requireNonNull(value, field);
            if (value.isZero() || value.isNegative()) {
                throw new IllegalArgumentException(
                        field + " must be positive"
                );
            }
            return value;
        }
    }
}

package top.egon.cola.component.gateway.admin.interfaces.management;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.application.observability.GatewayObservabilityQueryService;
import top.egon.cola.component.gateway.admin.application.observability.GatewayObservabilityStore;

/**
 * 中文说明：{@code GatewayObservabilityController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责网关可观测性控制器相关的职责与边界。
 * English summary: {@code GatewayObservabilityController} is a gateway observability controller controller in the current Gateway module; it owns the gateway observability controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
public class GatewayObservabilityController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code GatewayObservabilityQueryService}，由 {@code GatewayObservabilityController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code GatewayObservabilityQueryService}, and {@code GatewayObservabilityController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayObservabilityController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayObservabilityQueryService service;

    /**
     * 中文说明：创建 {@code GatewayObservabilityController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayObservabilityController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public GatewayObservabilityController(
            GatewayObservabilityQueryService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 dashboard 操作；该方法是 {@code GatewayObservabilityController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dashboard operation; this method is the invocation entry point on {@code GatewayObservabilityController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityController.dashboard(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param appCode 参数 appCode；parameter app code。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @return 返回 dashboard 的处理结果；returns the result of the operation.
     */
    @GetMapping("/dashboard")
    public GatewayObservabilityStore.DashboardSummary dashboard(
            @RequestParam String bizCode,
            @RequestParam String appCode,
            @RequestParam String env,
            @RequestParam String namespace) {
        return service.dashboard(bizCode, appCode, env, namespace);
    }

    /**
     * 中文说明：执行 traces 操作；该方法是 {@code GatewayObservabilityController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the traces operation; this method is the invocation entry point on {@code GatewayObservabilityController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityController.traces(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param traceId 参数 traceId；parameter trace id。
     * @param protocol 参数 protocol；parameter protocol。
     * @param statusCategory 参数 statusCategory；parameter status category。
     * @param page 参数 page；parameter page。
     * @param size 参数 size；parameter size。
     * @return 返回 traces 的处理结果；returns the result of the operation.
     */
    @GetMapping("/observability/traces")
    public GatewayObservabilityStore.Page<
            GatewayObservabilityStore.TraceSummary> traces(
            @RequestParam String env,
            @RequestParam String namespace,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String protocol,
            @RequestParam(required = false) String statusCategory,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.traces(new GatewayObservabilityStore.TraceQuery(
                env,
                namespace,
                traceId,
                protocol,
                statusCategory,
                page,
                size
        ));
    }

    /**
     * 中文说明：执行 audits 操作；该方法是 {@code GatewayObservabilityController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audits operation; this method is the invocation entry point on {@code GatewayObservabilityController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityController.audits(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param actorId 参数 actorId；parameter actor id。
     * @param resourceId 参数 资源Id；parameter resource id。
     * @param traceId 参数 traceId；parameter trace id。
     * @param successful 参数 successful；parameter successful。
     * @param page 参数 page；parameter page。
     * @param size 参数 size；parameter size。
     * @return 返回 audits 的处理结果；returns the result of the operation.
     */
    @GetMapping("/audit")
    public GatewayObservabilityStore.Page<
            GatewayObservabilityStore.AuditSummary> audits(
            @RequestParam String env,
            @RequestParam String namespace,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) Boolean successful,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.audits(new GatewayObservabilityStore.AuditQuery(
                env,
                namespace,
                actorId,
                resourceId,
                traceId,
                successful,
                page,
                size
        ));
    }
}

package top.egon.cola.component.gateway.admin.interfaces.management;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.application.routing.GatewayDraftService;
import top.egon.cola.component.gateway.admin.domain.AdminActor;

import java.util.Map;

/**
 * 中文说明：{@code GatewayDraftController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责网关草稿控制器相关的职责与边界。
 * English summary: {@code GatewayDraftController} is a gateway draft controller controller in the current Gateway module; it owns the gateway draft controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin/gateway-groups/{gatewayGroupId}/draft")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
public class GatewayDraftController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code GatewayDraftService}，由 {@code GatewayDraftController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code GatewayDraftService}, and {@code GatewayDraftController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDraftService service;

    /**
     * 中文说明：创建 {@code GatewayDraftController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayDraftController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public GatewayDraftController(GatewayDraftService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 get 操作；该方法是 {@code GatewayDraftController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get operation; this method is the invocation entry point on {@code GatewayDraftController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftController.get(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 get 的处理结果；returns the result of the operation.
     */
    @GetMapping
    public GatewayDraftService.DraftView get(
            @PathVariable String gatewayGroupId) {
        return service.get(gatewayGroupId);
    }

    /**
     * 中文说明：执行 put路由 操作；该方法是 {@code GatewayDraftController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put route operation; this method is the invocation entry point on {@code GatewayDraftController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftController.putRoute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param routeId 参数 路由Id；parameter route id。
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 put路由 的处理结果；returns the result of the operation.
     */
    @PutMapping("/routes/{routeId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:drafts:write','CAP_*')")
    public GatewayDraftService.MutationResult putRoute(
            @PathVariable String gatewayGroupId,
            @PathVariable String routeId,
            @Valid @RequestBody RouteRequest request,
            AdminActor actor) {
        return service.putRoute(
                gatewayGroupId,
                routeId,
                new GatewayDraftService.RouteMutation(
                        request.operationId(),
                        request.content(),
                        request.enabled(),
                        request.expectedRevision(),
                        request.idempotencyKey(),
                        request.changeReason()
                ),
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 delete路由 操作；该方法是 {@code GatewayDraftController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete route operation; this method is the invocation entry point on {@code GatewayDraftController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftController.deleteRoute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param routeId 参数 路由Id；parameter route id。
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 delete路由 的处理结果；returns the result of the operation.
     */
    @DeleteMapping("/routes/{routeId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:drafts:write','CAP_*')")
    public GatewayDraftService.MutationResult deleteRoute(
            @PathVariable String gatewayGroupId,
            @PathVariable String routeId,
            @Valid @RequestBody MutationRequest request,
            AdminActor actor) {
        return service.deleteRoute(
                gatewayGroupId,
                routeId,
                request.control(),
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 put策略 操作；该方法是 {@code GatewayDraftController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put policy operation; this method is the invocation entry point on {@code GatewayDraftController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftController.putPolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param policyId 参数 策略Id；parameter policy id。
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 put策略 的处理结果；returns the result of the operation.
     */
    @PutMapping("/policies/{policyId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:drafts:write','CAP_*')")
    public GatewayDraftService.MutationResult putPolicy(
            @PathVariable String gatewayGroupId,
            @PathVariable String policyId,
            @Valid @RequestBody PolicyRequest request,
            AdminActor actor) {
        return service.putPolicy(
                gatewayGroupId,
                policyId,
                new GatewayDraftService.PolicyMutation(
                        request.policyType(),
                        request.policyScope(),
                        request.content(),
                        request.enabled(),
                        request.expectedRevision(),
                        request.idempotencyKey(),
                        request.changeReason()
                ),
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 delete策略 操作；该方法是 {@code GatewayDraftController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete policy operation; this method is the invocation entry point on {@code GatewayDraftController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftController.deletePolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param policyId 参数 策略Id；parameter policy id。
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 delete策略 的处理结果；returns the result of the operation.
     */
    @DeleteMapping("/policies/{policyId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:drafts:write','CAP_*')")
    public GatewayDraftService.MutationResult deletePolicy(
            @PathVariable String gatewayGroupId,
            @PathVariable String policyId,
            @Valid @RequestBody MutationRequest request,
            AdminActor actor) {
        return service.deletePolicy(
                gatewayGroupId,
                policyId,
                request.control(),
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code GatewayDraftController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code GatewayDraftController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftController.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 validate 的处理结果；returns the result of the operation.
     */
    @PostMapping("/validate")
    public GatewayDraftService.ValidationReport validate(
            @PathVariable String gatewayGroupId) {
        return service.validate(gatewayGroupId);
    }

    /**
     * 中文说明：执行 diff 操作；该方法是 {@code GatewayDraftController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the diff operation; this method is the invocation entry point on {@code GatewayDraftController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftController.diff(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 diff 的处理结果；returns the result of the operation.
     */
    @GetMapping("/diff")
    public GatewayDraftService.DraftDiff diff(
            @PathVariable String gatewayGroupId) {
        return service.diff(gatewayGroupId);
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code GatewayDraftController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code GatewayDraftController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftController.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 审计 的处理结果；returns the result of the operation.
     */
    private RequestAuditContext audit() {
        return RequestAuditContext.current();
    }

    /**
     * 中文说明：{@code RouteRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责路由请求相关的职责与边界。
     * English summary: {@code RouteRequest} is an immutable data carrier in the current Gateway module; it owns the route request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record RouteRequest(
            /**
             * 中文说明：保存 操作Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftController.RouteRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation id; its type is {@code String}, and {@code GatewayDraftController.RouteRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftController.RouteRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController.RouteRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String operationId,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayDraftController.RouteRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code GatewayDraftController.RouteRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftController.RouteRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController.RouteRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotNull Map<String, Object> content,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayDraftController.RouteRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code GatewayDraftController.RouteRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftController.RouteRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController.RouteRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayDraftController.RouteRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code GatewayDraftController.RouteRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftController.RouteRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController.RouteRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedRevision,
            /**
             * 中文说明：保存 idempotency键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftController.RouteRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by idempotency key; its type is {@code String}, and {@code GatewayDraftController.RouteRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftController.RouteRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController.RouteRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String idempotencyKey,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftController.RouteRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code GatewayDraftController.RouteRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftController.RouteRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController.RouteRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String changeReason
    ) {
    }

    /**
     * 中文说明：{@code PolicyRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责策略请求相关的职责与边界。
     * English summary: {@code PolicyRequest} is an immutable data carrier in the current Gateway module; it owns the policy request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param policyType 参数 策略Type；parameter policy type。
     * @param policyScope 参数 策略Scope；parameter policy scope。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record PolicyRequest(
            /**
             * 中文说明：保存 策略Type 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftController.PolicyRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by policy type; its type is {@code String}, and {@code GatewayDraftController.PolicyRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftController.PolicyRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController.PolicyRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String policyType,
            /**
             * 中文说明：保存 策略Scope 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftController.PolicyRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by policy scope; its type is {@code String}, and {@code GatewayDraftController.PolicyRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftController.PolicyRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController.PolicyRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String policyScope,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayDraftController.PolicyRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code GatewayDraftController.PolicyRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftController.PolicyRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController.PolicyRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotNull Map<String, Object> content,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayDraftController.PolicyRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code GatewayDraftController.PolicyRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftController.PolicyRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController.PolicyRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayDraftController.PolicyRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code GatewayDraftController.PolicyRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftController.PolicyRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController.PolicyRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedRevision,
            /**
             * 中文说明：保存 idempotency键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftController.PolicyRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by idempotency key; its type is {@code String}, and {@code GatewayDraftController.PolicyRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftController.PolicyRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController.PolicyRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String idempotencyKey,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftController.PolicyRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code GatewayDraftController.PolicyRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftController.PolicyRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController.PolicyRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String changeReason
    ) {
    }

    /**
     * 中文说明：{@code MutationRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Mutation请求相关的职责与边界。
     * English summary: {@code MutationRequest} is an immutable data carrier in the current Gateway module; it owns the mutation request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record MutationRequest(
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayDraftController.MutationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code GatewayDraftController.MutationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftController.MutationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController.MutationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedRevision,
            /**
             * 中文说明：保存 idempotency键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftController.MutationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by idempotency key; its type is {@code String}, and {@code GatewayDraftController.MutationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftController.MutationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController.MutationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String idempotencyKey,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftController.MutationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code GatewayDraftController.MutationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftController.MutationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController.MutationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String changeReason
    ) {

        /**
         * 中文说明：执行 control 操作；该方法是 {@code GatewayDraftController.MutationRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the control operation; this method is the invocation entry point on {@code GatewayDraftController.MutationRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftController.MutationRequest.control(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 control 的处理结果；returns the result of the operation.
         */
        private GatewayDraftService.MutationControl control() {
            return new GatewayDraftService.MutationControl(
                    expectedRevision,
                    idempotencyKey,
                    changeReason
            );
        }
    }
}

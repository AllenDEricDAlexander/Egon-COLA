package top.egon.cola.component.gateway.admin.mcp.interfaces;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.mcp.application.McpControlPlaneService;
import top.egon.cola.component.gateway.admin.mcp.application.McpValidationService;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpCapabilityDraftStore;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 中文说明：{@code McpCapabilityController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责MCPCapability控制器相关的职责与边界。
 * English summary: {@code McpCapabilityController} is a mcp capability controller controller in the current Gateway module; it owns the mcp capability controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin/mcp")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:read','CAP_*')")
public class McpCapabilityController {

    /**
     * 中文说明：表示 CAPABILITYCOLLECTION 这一固定值；它属于 {@code McpCapabilityController} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value capability collection; it is a state, type, or protocol value of {@code McpCapabilityController} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpCapabilityController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilityController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String CAPABILITY_COLLECTION =
            "{plural:resources|resource-templates|prompts|"
                    + "task-policies|app-bindings}";

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code McpControlPlaneService}，由 {@code McpCapabilityController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code McpControlPlaneService}, and {@code McpCapabilityController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpCapabilityController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilityController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpControlPlaneService service;

    /**
     * 中文说明：创建 {@code McpCapabilityController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpCapabilityController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public McpCapabilityController(McpControlPlaneService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 list 操作；该方法是 {@code McpCapabilityController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code McpCapabilityController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilityController.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverId 参数 服务器Id；parameter server id。
     * @param plural 参数 plural；parameter plural。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    @GetMapping("/servers/{serverId}/" + CAPABILITY_COLLECTION)
    public List<JdbcMcpCapabilityDraftStore.CapabilityDraft> list(
            @PathVariable String serverId,
            @PathVariable String plural,
            @RequestParam String gatewayGroupId) {
        return service.capabilities(
                gatewayGroupId,
                serverId,
                kind(plural)
        );
    }

    /**
     * 中文说明：执行 create 操作；该方法是 {@code McpCapabilityController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code McpCapabilityController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilityController.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverId 参数 服务器Id；parameter server id。
     * @param plural 参数 plural；parameter plural。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
    @PostMapping("/servers/{serverId}/" + CAPABILITY_COLLECTION)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult create(
            @PathVariable String serverId,
            @PathVariable String plural,
            @Valid @RequestBody CapabilityRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putCapability(
                null,
                kind(plural),
                request.mutation(serverId),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 update 操作；该方法是 {@code McpCapabilityController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update operation; this method is the invocation entry point on {@code McpCapabilityController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilityController.update(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param plural 参数 plural；parameter plural。
     * @param id 参数 id；parameter id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 update 的处理结果；returns the result of the operation.
     */
    @PutMapping("/" + CAPABILITY_COLLECTION + "/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult update(
            @PathVariable String plural,
            @PathVariable String id,
            @Valid @RequestBody CapabilityRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putCapability(
                id,
                kind(plural),
                request.mutation(request.serverId()),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 delete 操作；该方法是 {@code McpCapabilityController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete operation; this method is the invocation entry point on {@code McpCapabilityController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilityController.delete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param plural 参数 plural；parameter plural。
     * @param id 参数 id；parameter id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 delete 的处理结果；returns the result of the operation.
     */
    @DeleteMapping("/" + CAPABILITY_COLLECTION + "/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult delete(
            @PathVariable String plural,
            @PathVariable String id,
            @Valid @RequestBody McpServerController.MutationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.deleteCapability(
                id,
                kind(plural),
                new McpControlPlaneService.MutationControl(
                        request.gatewayGroupId(),
                        request.expectedRevision(),
                        request.expectedDraftRevision(),
                        request.changeReason()
                ),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code McpCapabilityController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code McpCapabilityController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilityController.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param plural 参数 plural；parameter plural。
     * @param id 参数 id；parameter id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 validate 的处理结果；returns the result of the operation.
     */
    @PostMapping("/" + CAPABILITY_COLLECTION + "/{id}/validate")
    public McpValidationService.ValidationReport validate(
            @PathVariable String plural,
            @PathVariable String id,
            @RequestParam String gatewayGroupId) {
        kind(plural);
        return service.validate(gatewayGroupId);
    }

    /**
     * 中文说明：执行 kind 操作；该方法是 {@code McpCapabilityController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the kind operation; this method is the invocation entry point on {@code McpCapabilityController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilityController.kind(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 kind 的处理结果；returns the result of the operation.
     */
    private JdbcMcpCapabilityDraftStore.CapabilityKind kind(String value) {
        String normalized = value.toUpperCase(Locale.ROOT)
                .replace('-', '_');
        return switch (normalized) {
            case "RESOURCES" ->
                    JdbcMcpCapabilityDraftStore.CapabilityKind.RESOURCE;
            case "RESOURCE_TEMPLATES" ->
                    JdbcMcpCapabilityDraftStore.CapabilityKind
                            .RESOURCE_TEMPLATE;
            case "PROMPTS" ->
                    JdbcMcpCapabilityDraftStore.CapabilityKind.PROMPT;
            case "TASK_POLICIES" ->
                    JdbcMcpCapabilityDraftStore.CapabilityKind.TASK_POLICY;
            case "APP_BINDINGS" ->
                    JdbcMcpCapabilityDraftStore.CapabilityKind.APP_BINDING;
            default -> throw new IllegalArgumentException(
                    "unsupported MCP capability collection: " + value
            );
        };
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code McpCapabilityController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code McpCapabilityController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilityController.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 审计 的处理结果；returns the result of the operation.
     */
    private RequestAuditContext audit() {
        return RequestAuditContext.current();
    }

    /**
     * 中文说明：{@code CapabilityRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Capability请求相关的职责与边界。
     * English summary: {@code CapabilityRequest} is an immutable data carrier in the current Gateway module; it owns the capability request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param name 参数 name；parameter name。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record CapabilityRequest(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCapabilityController.CapabilityRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpCapabilityController.CapabilityRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilityController.CapabilityRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilityController.CapabilityRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String gatewayGroupId,
            /**
             * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCapabilityController.CapabilityRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code McpCapabilityController.CapabilityRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilityController.CapabilityRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilityController.CapabilityRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverId,
            /**
             * 中文说明：保存 name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCapabilityController.CapabilityRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by name; its type is {@code String}, and {@code McpCapabilityController.CapabilityRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilityController.CapabilityRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilityController.CapabilityRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String name,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpCapabilityController.CapabilityRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code McpCapabilityController.CapabilityRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilityController.CapabilityRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilityController.CapabilityRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotNull Map<String, Object> content,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpCapabilityController.CapabilityRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code McpCapabilityController.CapabilityRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilityController.CapabilityRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilityController.CapabilityRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpCapabilityController.CapabilityRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpCapabilityController.CapabilityRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilityController.CapabilityRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilityController.CapabilityRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpCapabilityController.CapabilityRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpCapabilityController.CapabilityRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilityController.CapabilityRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilityController.CapabilityRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCapabilityController.CapabilityRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpCapabilityController.CapabilityRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCapabilityController.CapabilityRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCapabilityController.CapabilityRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String changeReason
    ) {

        /**
         * 中文说明：执行 mutation 操作；该方法是 {@code McpCapabilityController.CapabilityRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the mutation operation; this method is the invocation entry point on {@code McpCapabilityController.CapabilityRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilityController.CapabilityRequest.mutation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param resolvedServerId 参数 resolved服务器Id；parameter resolved server id。
         * @return 返回 mutation 的处理结果；returns the result of the operation.
         */
        private McpControlPlaneService.CapabilityMutation mutation(
                String resolvedServerId) {
            return new McpControlPlaneService.CapabilityMutation(
                    gatewayGroupId,
                    resolvedServerId,
                    name,
                    content,
                    enabled,
                    expectedRevision,
                    expectedDraftRevision,
                    changeReason
            );
        }
    }
}

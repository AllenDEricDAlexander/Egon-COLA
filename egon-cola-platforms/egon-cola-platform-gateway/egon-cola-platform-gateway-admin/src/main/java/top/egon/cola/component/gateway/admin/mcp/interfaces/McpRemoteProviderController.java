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
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpRemoteProviderStore;

import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code McpRemoteProviderController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责MCP远程提供方控制器相关的职责与边界。
 * English summary: {@code McpRemoteProviderController} is a mcp remote provider controller controller in the current Gateway module; it owns the mcp remote provider controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin/mcp/remote")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:read','CAP_*')")
public class McpRemoteProviderController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code McpControlPlaneService}，由 {@code McpRemoteProviderController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code McpControlPlaneService}, and {@code McpRemoteProviderController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpControlPlaneService service;

    /**
     * 中文说明：创建 {@code McpRemoteProviderController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpRemoteProviderController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public McpRemoteProviderController(McpControlPlaneService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 providers 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the providers operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.providers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 providers 的处理结果；returns the result of the operation.
     */
    @GetMapping("/providers")
    public List<JdbcMcpRemoteProviderStore.RemoteProviderDraft> providers(
            @RequestParam String gatewayGroupId) {
        return service.providers(gatewayGroupId);
    }

    /**
     * 中文说明：执行 create提供方 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create provider operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.createProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 create提供方 的处理结果；returns the result of the operation.
     */
    @PostMapping("/providers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult createProvider(
            @Valid @RequestBody ProviderRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putProvider(
                null,
                request.mutation(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 update提供方 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update provider operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.updateProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 update提供方 的处理结果；returns the result of the operation.
     */
    @PutMapping("/providers/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult updateProvider(
            @PathVariable String id,
            @Valid @RequestBody ProviderRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putProvider(
                id,
                request.mutation(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 delete提供方 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete provider operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.deleteProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 delete提供方 的处理结果；returns the result of the operation.
     */
    @DeleteMapping("/providers/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult deleteProvider(
            @PathVariable String id,
            @Valid @RequestBody McpServerController.MutationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.deleteProvider(
                id,
                control(request),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 discover 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the discover operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.discover(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 discover 的处理结果；returns the result of the operation.
     */
    @PostMapping("/providers/{id}/discover")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:test','CAP_*')")
    public List<JdbcMcpRemoteProviderStore.RemoteCapability> discover(
            @PathVariable String id) {
        return service.remoteCapabilities(id);
    }

    /**
     * 中文说明：执行 mounts 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mounts operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.mounts(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 mounts 的处理结果；returns the result of the operation.
     */
    @GetMapping("/mounts")
    public List<JdbcMcpRemoteProviderStore.RemoteMountDraft> mounts(
            @RequestParam String gatewayGroupId) {
        return service.mounts(gatewayGroupId);
    }

    /**
     * 中文说明：执行 createMount 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create mount operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.createMount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 createMount 的处理结果；returns the result of the operation.
     */
    @PostMapping("/mounts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult createMount(
            @Valid @RequestBody MountRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putMount(
                null,
                request.mutation(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 updateMount 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update mount operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.updateMount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 updateMount 的处理结果；returns the result of the operation.
     */
    @PutMapping("/mounts/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult updateMount(
            @PathVariable String id,
            @Valid @RequestBody MountRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putMount(
                id,
                request.mutation(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 deleteMount 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete mount operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.deleteMount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 deleteMount 的处理结果；returns the result of the operation.
     */
    @DeleteMapping("/mounts/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult deleteMount(
            @PathVariable String id,
            @Valid @RequestBody McpServerController.MutationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.deleteMount(
                id,
                control(request),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 control 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the control operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.control(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 control 的处理结果；returns the result of the operation.
     */
    private McpControlPlaneService.MutationControl control(
            McpServerController.MutationRequest request) {
        return new McpControlPlaneService.MutationControl(
                request.gatewayGroupId(),
                request.expectedRevision(),
                request.expectedDraftRevision(),
                request.changeReason()
        );
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 审计 的处理结果；returns the result of the operation.
     */
    private RequestAuditContext audit() {
        return RequestAuditContext.current();
    }

    /**
     * 中文说明：{@code ProviderRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责提供方请求相关的职责与边界。
     * English summary: {@code ProviderRequest} is an immutable data carrier in the current Gateway module; it owns the provider request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param providerCode 参数 提供方Code；parameter provider code。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record ProviderRequest(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpRemoteProviderController.ProviderRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpRemoteProviderController.ProviderRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController.ProviderRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController.ProviderRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String gatewayGroupId,
            /**
             * 中文说明：保存 提供方Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpRemoteProviderController.ProviderRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by provider code; its type is {@code String}, and {@code McpRemoteProviderController.ProviderRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController.ProviderRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController.ProviderRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String providerCode,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpRemoteProviderController.ProviderRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code McpRemoteProviderController.ProviderRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController.ProviderRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController.ProviderRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotNull Map<String, Object> content,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpRemoteProviderController.ProviderRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code McpRemoteProviderController.ProviderRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController.ProviderRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController.ProviderRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpRemoteProviderController.ProviderRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpRemoteProviderController.ProviderRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController.ProviderRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController.ProviderRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpRemoteProviderController.ProviderRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpRemoteProviderController.ProviderRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController.ProviderRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController.ProviderRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpRemoteProviderController.ProviderRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpRemoteProviderController.ProviderRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController.ProviderRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController.ProviderRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String changeReason
    ) {

        /**
         * 中文说明：执行 mutation 操作；该方法是 {@code McpRemoteProviderController.ProviderRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the mutation operation; this method is the invocation entry point on {@code McpRemoteProviderController.ProviderRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.ProviderRequest.mutation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 mutation 的处理结果；returns the result of the operation.
         */
        private McpControlPlaneService.RemoteProviderMutation mutation() {
            return new McpControlPlaneService.RemoteProviderMutation(
                    gatewayGroupId,
                    providerCode,
                    content,
                    enabled,
                    expectedRevision,
                    expectedDraftRevision,
                    changeReason
            );
        }
    }

    /**
     * 中文说明：{@code MountRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Mount请求相关的职责与边界。
     * English summary: {@code MountRequest} is an immutable data carrier in the current Gateway module; it owns the mount request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param providerId 参数 提供方Id；parameter provider id。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param capabilityFingerprint 参数 capabilityFingerprint；parameter capability fingerprint。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record MountRequest(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpRemoteProviderController.MountRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpRemoteProviderController.MountRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController.MountRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController.MountRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String gatewayGroupId,
            /**
             * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpRemoteProviderController.MountRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code McpRemoteProviderController.MountRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController.MountRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController.MountRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String serverId,
            /**
             * 中文说明：保存 提供方Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpRemoteProviderController.MountRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by provider id; its type is {@code String}, and {@code McpRemoteProviderController.MountRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController.MountRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController.MountRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String providerId,
            /**
             * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpRemoteProviderController.MountRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code McpRemoteProviderController.MountRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController.MountRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController.MountRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String namespace,
            /**
             * 中文说明：保存 capabilityFingerprint 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpRemoteProviderController.MountRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by capability fingerprint; its type is {@code String}, and {@code McpRemoteProviderController.MountRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController.MountRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController.MountRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String capabilityFingerprint,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpRemoteProviderController.MountRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code McpRemoteProviderController.MountRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController.MountRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController.MountRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotNull Map<String, Object> content,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpRemoteProviderController.MountRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code McpRemoteProviderController.MountRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController.MountRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController.MountRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpRemoteProviderController.MountRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpRemoteProviderController.MountRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController.MountRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController.MountRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpRemoteProviderController.MountRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpRemoteProviderController.MountRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController.MountRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController.MountRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpRemoteProviderController.MountRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpRemoteProviderController.MountRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController.MountRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController.MountRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String changeReason
    ) {

        /**
         * 中文说明：执行 mutation 操作；该方法是 {@code McpRemoteProviderController.MountRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the mutation operation; this method is the invocation entry point on {@code McpRemoteProviderController.MountRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.MountRequest.mutation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 mutation 的处理结果；returns the result of the operation.
         */
        private McpControlPlaneService.RemoteMountMutation mutation() {
            return new McpControlPlaneService.RemoteMountMutation(
                    gatewayGroupId,
                    serverId,
                    providerId,
                    namespace,
                    capabilityFingerprint,
                    content,
                    enabled,
                    expectedRevision,
                    expectedDraftRevision,
                    changeReason
            );
        }
    }
}

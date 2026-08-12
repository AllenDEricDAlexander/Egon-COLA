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
import top.egon.cola.component.gateway.admin.mcp.application.McpToolAdminService;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 中文说明：{@code McpToolAdminController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责MCP工具管理端控制器相关的职责与边界。
 * English summary: {@code McpToolAdminController} is a mcp tool admin controller controller in the current Gateway module; it owns the mcp tool admin controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin/mcp")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:read','CAP_*')")
public class McpToolAdminController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code McpToolAdminService}，由 {@code McpToolAdminController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code McpToolAdminService}, and {@code McpToolAdminController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolAdminController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpToolAdminService service;

    /**
     * 中文说明：创建 {@code McpToolAdminController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpToolAdminController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public McpToolAdminController(McpToolAdminService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 managedTools 操作；该方法是 {@code McpToolAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the managed tools operation; this method is the invocation entry point on {@code McpToolAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminController.managedTools(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param groupId 参数 groupId；parameter group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @return 返回 managedTools 的处理结果；returns the result of the operation.
     */
    @GetMapping("/groups/{groupId}/managed-tools")
    public List<McpToolAdminService.ManagedToolView> managedTools(
            @PathVariable String groupId,
            @RequestParam(required = false) String serverId) {
        return service.managedTools(groupId, serverId);
    }

    /**
     * 中文说明：执行 putOverride 操作；该方法是 {@code McpToolAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put override operation; this method is the invocation entry point on {@code McpToolAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminController.putOverride(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param toolId 参数 工具Id；parameter tool id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 putOverride 的处理结果；returns the result of the operation.
     */
    @PutMapping("/managed-tools/{toolId}/override")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult putOverride(
            @PathVariable String toolId,
            @Valid @RequestBody ManagedToolOverrideRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putOverride(
                toolId,
                request.mutation(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    /**
     * 中文说明：执行 deleteOverride 操作；该方法是 {@code McpToolAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete override operation; this method is the invocation entry point on {@code McpToolAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminController.deleteOverride(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param toolId 参数 工具Id；parameter tool id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 deleteOverride 的处理结果；returns the result of the operation.
     */
    @DeleteMapping("/managed-tools/{toolId}/override")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult deleteOverride(
            @PathVariable String toolId,
            @Valid @RequestBody MutationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.deleteOverride(
                toolId,
                request.control(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    /**
     * 中文说明：执行 远程Tools 操作；该方法是 {@code McpToolAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the remote tools operation; this method is the invocation entry point on {@code McpToolAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminController.remoteTools(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @return 返回 远程Tools 的处理结果；returns the result of the operation.
     */
    @GetMapping("/remote-tools")
    public List<McpToolAdminService.RemoteToolView> remoteTools(
            @RequestParam String gatewayGroupId,
            @RequestParam(required = false) String serverId) {
        return service.remoteTools(gatewayGroupId, serverId);
    }

    /**
     * 中文说明：执行 create远程工具 操作；该方法是 {@code McpToolAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create remote tool operation; this method is the invocation entry point on {@code McpToolAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminController.createRemoteTool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 create远程工具 的处理结果；returns the result of the operation.
     */
    @PostMapping("/remote-tools")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult createRemoteTool(
            @Valid @RequestBody RemoteToolRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putRemoteTool(
                null,
                request.mutation(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    /**
     * 中文说明：执行 update远程工具 操作；该方法是 {@code McpToolAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update remote tool operation; this method is the invocation entry point on {@code McpToolAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminController.updateRemoteTool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param toolId 参数 工具Id；parameter tool id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 update远程工具 的处理结果；returns the result of the operation.
     */
    @PutMapping("/remote-tools/{toolId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult updateRemoteTool(
            @PathVariable String toolId,
            @Valid @RequestBody RemoteToolRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putRemoteTool(
                toolId,
                request.mutation(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    /**
     * 中文说明：执行 delete远程工具 操作；该方法是 {@code McpToolAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete remote tool operation; this method is the invocation entry point on {@code McpToolAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminController.deleteRemoteTool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param toolId 参数 工具Id；parameter tool id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 delete远程工具 的处理结果；returns the result of the operation.
     */
    @DeleteMapping("/remote-tools/{toolId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult deleteRemoteTool(
            @PathVariable String toolId,
            @Valid @RequestBody MutationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.deleteRemoteTool(
                toolId,
                request.control(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    /**
     * 中文说明：{@code ManagedToolOverrideRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Managed工具Override请求相关的职责与边界。
     * English summary: {@code ManagedToolOverrideRequest} is an immutable data carrier in the current Gateway module; it owns the managed tool override request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param enabled 参数 enabled；parameter enabled。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param additionalPermissions 参数 additionalPermissions；parameter additional permissions。
     * @param minimumRiskLevel 参数 minimumRiskLevel；parameter minimum risk level。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record ManagedToolOverrideRequest(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminController.ManagedToolOverrideRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpToolAdminController.ManagedToolOverrideRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.ManagedToolOverrideRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.ManagedToolOverrideRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String gatewayGroupId,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code Boolean}，由 {@code McpToolAdminController.ManagedToolOverrideRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code Boolean}, and {@code McpToolAdminController.ManagedToolOverrideRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.ManagedToolOverrideRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.ManagedToolOverrideRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Boolean enabled,
            /**
             * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminController.ManagedToolOverrideRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code McpToolAdminController.ManagedToolOverrideRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.ManagedToolOverrideRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.ManagedToolOverrideRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverId,
            /**
             * 中文说明：保存 additionalPermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpToolAdminController.ManagedToolOverrideRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by additional permissions; its type is {@code Set<String>}, and {@code McpToolAdminController.ManagedToolOverrideRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.ManagedToolOverrideRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.ManagedToolOverrideRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> additionalPermissions,
            /**
             * 中文说明：保存 minimumRiskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminController.ManagedToolOverrideRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by minimum risk level; its type is {@code String}, and {@code McpToolAdminController.ManagedToolOverrideRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.ManagedToolOverrideRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.ManagedToolOverrideRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String minimumRiskLevel,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpToolAdminController.ManagedToolOverrideRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpToolAdminController.ManagedToolOverrideRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.ManagedToolOverrideRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.ManagedToolOverrideRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpToolAdminController.ManagedToolOverrideRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpToolAdminController.ManagedToolOverrideRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.ManagedToolOverrideRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.ManagedToolOverrideRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminController.ManagedToolOverrideRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpToolAdminController.ManagedToolOverrideRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.ManagedToolOverrideRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.ManagedToolOverrideRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String changeReason
    ) {

        /**
         * 中文说明：执行 mutation 操作；该方法是 {@code McpToolAdminController.ManagedToolOverrideRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the mutation operation; this method is the invocation entry point on {@code McpToolAdminController.ManagedToolOverrideRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminController.ManagedToolOverrideRequest.mutation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 mutation 的处理结果；returns the result of the operation.
         */
        private McpToolAdminService.ManagedToolOverrideMutation mutation() {
            return new McpToolAdminService.ManagedToolOverrideMutation(
                    gatewayGroupId,
                    enabled,
                    serverId,
                    additionalPermissions,
                    minimumRiskLevel,
                    expectedRevision,
                    expectedDraftRevision,
                    changeReason
            );
        }
    }

    /**
     * 中文说明：{@code RemoteToolRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责远程工具请求相关的职责与边界。
     * English summary: {@code RemoteToolRequest} is an immutable data carrier in the current Gateway module; it owns the remote tool request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param name 参数 name；parameter name。
     * @param description 参数 description；parameter description。
     * @param remoteMountId 参数 远程MountId；parameter remote mount id。
     * @param inputSchema 参数 input模式；parameter input schema。
     * @param outputSchema 参数 output模式；parameter output schema。
     * @param annotations 参数 annotations；parameter annotations。
     * @param requiredPermissions 参数 requiredPermissions；parameter required permissions。
     * @param riskLevel 参数 riskLevel；parameter risk level。
     * @param idempotent 参数 idempotent；parameter idempotent。
     * @param enabled 参数 enabled；parameter enabled。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record RemoteToolRequest(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminController.RemoteToolRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpToolAdminController.RemoteToolRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.RemoteToolRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.RemoteToolRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String gatewayGroupId,
            /**
             * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminController.RemoteToolRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code McpToolAdminController.RemoteToolRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.RemoteToolRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.RemoteToolRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String serverId,
            /**
             * 中文说明：保存 name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminController.RemoteToolRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by name; its type is {@code String}, and {@code McpToolAdminController.RemoteToolRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.RemoteToolRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.RemoteToolRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String name,
            /**
             * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminController.RemoteToolRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code McpToolAdminController.RemoteToolRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.RemoteToolRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.RemoteToolRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String description,
            /**
             * 中文说明：保存 远程MountId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminController.RemoteToolRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by remote mount id; its type is {@code String}, and {@code McpToolAdminController.RemoteToolRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.RemoteToolRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.RemoteToolRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String remoteMountId,
            /**
             * 中文说明：保存 input模式 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code McpToolAdminController.RemoteToolRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by input schema; its type is {@code Object}, and {@code McpToolAdminController.RemoteToolRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.RemoteToolRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.RemoteToolRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Object inputSchema,
            /**
             * 中文说明：保存 output模式 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code McpToolAdminController.RemoteToolRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by output schema; its type is {@code Object}, and {@code McpToolAdminController.RemoteToolRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.RemoteToolRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.RemoteToolRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Object outputSchema,
            /**
             * 中文说明：保存 annotations 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code McpToolAdminController.RemoteToolRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by annotations; its type is {@code Map<String, String>}, and {@code McpToolAdminController.RemoteToolRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.RemoteToolRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.RemoteToolRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, String> annotations,
            /**
             * 中文说明：保存 requiredPermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpToolAdminController.RemoteToolRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by required permissions; its type is {@code Set<String>}, and {@code McpToolAdminController.RemoteToolRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.RemoteToolRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.RemoteToolRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> requiredPermissions,
            /**
             * 中文说明：保存 riskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminController.RemoteToolRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by risk level; its type is {@code String}, and {@code McpToolAdminController.RemoteToolRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.RemoteToolRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.RemoteToolRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String riskLevel,
            /**
             * 中文说明：保存 idempotent 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpToolAdminController.RemoteToolRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by idempotent; its type is {@code boolean}, and {@code McpToolAdminController.RemoteToolRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.RemoteToolRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.RemoteToolRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean idempotent,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code Boolean}，由 {@code McpToolAdminController.RemoteToolRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code Boolean}, and {@code McpToolAdminController.RemoteToolRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.RemoteToolRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.RemoteToolRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotNull Boolean enabled,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpToolAdminController.RemoteToolRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpToolAdminController.RemoteToolRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.RemoteToolRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.RemoteToolRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpToolAdminController.RemoteToolRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpToolAdminController.RemoteToolRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.RemoteToolRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.RemoteToolRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminController.RemoteToolRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpToolAdminController.RemoteToolRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.RemoteToolRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.RemoteToolRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String changeReason
    ) {

        /**
         * 中文说明：执行 mutation 操作；该方法是 {@code McpToolAdminController.RemoteToolRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the mutation operation; this method is the invocation entry point on {@code McpToolAdminController.RemoteToolRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminController.RemoteToolRequest.mutation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 mutation 的处理结果；returns the result of the operation.
         */
        private McpToolAdminService.RemoteToolMutation mutation() {
            return new McpToolAdminService.RemoteToolMutation(
                    gatewayGroupId,
                    serverId,
                    name,
                    description,
                    remoteMountId,
                    inputSchema,
                    outputSchema,
                    annotations,
                    requiredPermissions,
                    riskLevel,
                    idempotent,
                    enabled,
                    expectedRevision,
                    expectedDraftRevision,
                    changeReason
            );
        }
    }

    /**
     * 中文说明：{@code MutationRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Mutation请求相关的职责与边界。
     * English summary: {@code MutationRequest} is an immutable data carrier in the current Gateway module; it owns the mutation request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record MutationRequest(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminController.MutationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpToolAdminController.MutationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.MutationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.MutationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String gatewayGroupId,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpToolAdminController.MutationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpToolAdminController.MutationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.MutationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.MutationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpToolAdminController.MutationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpToolAdminController.MutationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.MutationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.MutationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminController.MutationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpToolAdminController.MutationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminController.MutationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController.MutationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String changeReason
    ) {

        /**
         * 中文说明：执行 control 操作；该方法是 {@code McpToolAdminController.MutationRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the control operation; this method is the invocation entry point on {@code McpToolAdminController.MutationRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminController.MutationRequest.control(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 control 的处理结果；returns the result of the operation.
         */
        private McpToolAdminService.MutationControl control() {
            return new McpToolAdminService.MutationControl(
                    gatewayGroupId,
                    expectedRevision,
                    expectedDraftRevision,
                    changeReason
            );
        }
    }
}

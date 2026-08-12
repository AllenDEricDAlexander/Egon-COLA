package top.egon.cola.component.gateway.admin.mcp.interfaces;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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

import java.util.List;
import java.util.Set;

/**
 * 中文说明：{@code McpServerController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责MCP服务器控制器相关的职责与边界。
 * English summary: {@code McpServerController} is a mcp server controller controller in the current Gateway module; it owns the mcp server controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Validated
@RestController
@RequestMapping("/api/v1/gateway/admin/mcp/servers")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:read','CAP_*')")
public class McpServerController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code McpControlPlaneService}，由 {@code McpServerController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code McpControlPlaneService}, and {@code McpServerController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpServerController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpControlPlaneService service;

    /**
     * 中文说明：创建 {@code McpServerController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpServerController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public McpServerController(McpControlPlaneService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 list 操作；该方法是 {@code McpServerController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code McpServerController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerController.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    @GetMapping
    public List<McpControlPlaneService.ServerView> list(
            @RequestParam String gatewayGroupId) {
        return service.listServers(gatewayGroupId);
    }

    /**
     * 中文说明：执行 create 操作；该方法是 {@code McpServerController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code McpServerController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerController.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult create(
            @Valid @RequestBody ServerRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.createServer(
                request.mutation(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 get 操作；该方法是 {@code McpServerController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get operation; this method is the invocation entry point on {@code McpServerController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerController.get(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 get 的处理结果；returns the result of the operation.
     */
    @GetMapping("/{id}")
    public McpControlPlaneService.ServerView get(@PathVariable String id) {
        return service.getServer(id);
    }

    /**
     * 中文说明：执行 update 操作；该方法是 {@code McpServerController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update operation; this method is the invocation entry point on {@code McpServerController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerController.update(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 update 的处理结果；returns the result of the operation.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult update(
            @PathVariable String id,
            @Valid @RequestBody ServerRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.updateServer(
                id,
                request.mutation(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 delete 操作；该方法是 {@code McpServerController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete operation; this method is the invocation entry point on {@code McpServerController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerController.delete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 delete 的处理结果；returns the result of the operation.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult delete(
            @PathVariable String id,
            @Valid @RequestBody MutationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.deleteServer(
                id,
                request.control(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code McpServerController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code McpServerController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerController.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 validate 的处理结果；returns the result of the operation.
     */
    @PostMapping("/{id}/validate")
    public McpValidationService.ValidationReport validate(
            @PathVariable String id) {
        return service.validate(service.getServer(id).gatewayGroupId());
    }

    /**
     * 中文说明：执行 preview 操作；该方法是 {@code McpServerController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the preview operation; this method is the invocation entry point on {@code McpServerController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerController.preview(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 preview 的处理结果；returns the result of the operation.
     */
    @GetMapping("/{id}/capability-preview")
    public McpControlPlaneService.Preview preview(@PathVariable String id) {
        return service.preview(service.getServer(id).gatewayGroupId());
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code McpServerController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code McpServerController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerController.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 审计 的处理结果；returns the result of the operation.
     */
    private RequestAuditContext audit() {
        return RequestAuditContext.current();
    }

    /**
     * 中文说明：{@code ServerRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责服务器请求相关的职责与边界。
     * English summary: {@code ServerRequest} is an immutable data carrier in the current Gateway module; it owns the server request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param displayName 参数 displayName；parameter display name。
     * @param description 参数 description；parameter description。
     * @param instructions 参数 instructions；parameter instructions。
     * @param dialects 参数 dialects；parameter dialects。
     * @param resourceUri 参数 资源Uri；parameter resource uri。
     * @param listCacheTtlSeconds 参数 listCacheTtlSeconds；parameter list cache ttl seconds。
     * @param enabled 参数 enabled；parameter enabled。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record ServerRequest(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerController.ServerRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpServerController.ServerRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerController.ServerRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerController.ServerRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String gatewayGroupId,
            /**
             * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerController.ServerRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code McpServerController.ServerRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerController.ServerRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerController.ServerRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String serverCode,
            /**
             * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerController.ServerRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code McpServerController.ServerRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerController.ServerRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerController.ServerRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String displayName,
            /**
             * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerController.ServerRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code McpServerController.ServerRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerController.ServerRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerController.ServerRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String description,
            /**
             * 中文说明：保存 instructions 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerController.ServerRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by instructions; its type is {@code String}, and {@code McpServerController.ServerRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerController.ServerRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerController.ServerRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String instructions,
            /**
             * 中文说明：保存 dialects 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpServerController.ServerRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by dialects; its type is {@code Set<String>}, and {@code McpServerController.ServerRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerController.ServerRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerController.ServerRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotEmpty Set<String> dialects,
            /**
             * 中文说明：保存 资源Uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerController.ServerRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by resource uri; its type is {@code String}, and {@code McpServerController.ServerRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerController.ServerRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerController.ServerRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String resourceUri,
            /**
             * 中文说明：保存 listCacheTtlSeconds 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpServerController.ServerRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by list cache ttl seconds; its type is {@code long}, and {@code McpServerController.ServerRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerController.ServerRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerController.ServerRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long listCacheTtlSeconds,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code Boolean}，由 {@code McpServerController.ServerRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code Boolean}, and {@code McpServerController.ServerRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerController.ServerRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerController.ServerRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Boolean enabled,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpServerController.ServerRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpServerController.ServerRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerController.ServerRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerController.ServerRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpServerController.ServerRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpServerController.ServerRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerController.ServerRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerController.ServerRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerController.ServerRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpServerController.ServerRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerController.ServerRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerController.ServerRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String changeReason
    ) {

        /**
         * 中文说明：执行 mutation 操作；该方法是 {@code McpServerController.ServerRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the mutation operation; this method is the invocation entry point on {@code McpServerController.ServerRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpServerController.ServerRequest.mutation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 mutation 的处理结果；returns the result of the operation.
         */
        private McpControlPlaneService.ServerMutation mutation() {
            return new McpControlPlaneService.ServerMutation(
                    gatewayGroupId,
                    serverCode,
                    displayName,
                    description,
                    instructions,
                    dialects,
                    resourceUri,
                    listCacheTtlSeconds,
                    enabled == null || enabled,
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
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerController.MutationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpServerController.MutationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerController.MutationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerController.MutationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String gatewayGroupId,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpServerController.MutationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpServerController.MutationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerController.MutationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerController.MutationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpServerController.MutationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpServerController.MutationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerController.MutationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerController.MutationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpServerController.MutationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpServerController.MutationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpServerController.MutationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpServerController.MutationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String changeReason
    ) {

        /**
         * 中文说明：执行 control 操作；该方法是 {@code McpServerController.MutationRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the control operation; this method is the invocation entry point on {@code McpServerController.MutationRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpServerController.MutationRequest.control(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 control 的处理结果；returns the result of the operation.
         */
        private McpControlPlaneService.MutationControl control() {
            return new McpControlPlaneService.MutationControl(
                    gatewayGroupId,
                    expectedRevision,
                    expectedDraftRevision,
                    changeReason
            );
        }
    }
}

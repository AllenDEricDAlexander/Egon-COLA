package top.egon.cola.component.gateway.admin.interfaces.management;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.application.GatewayGroupService;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.domain.AdminActor;

import java.util.List;

/**
 * 中文说明：{@code GatewayGroupController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责网关Group控制器相关的职责与边界。
 * English summary: {@code GatewayGroupController} is a gateway group controller controller in the current Gateway module; it owns the gateway group controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin/gateway-groups")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
public class GatewayGroupController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code GatewayGroupService}，由 {@code GatewayGroupController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code GatewayGroupService}, and {@code GatewayGroupController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayGroupService service;

    /**
     * 中文说明：创建 {@code GatewayGroupController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayGroupController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public GatewayGroupController(GatewayGroupService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 list 操作；该方法是 {@code GatewayGroupController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code GatewayGroupController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupController.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    @GetMapping
    public List<GatewayGroupService.GatewayGroupView> list() {
        return service.list();
    }

    /**
     * 中文说明：执行 create 操作；该方法是 {@code GatewayGroupController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code GatewayGroupController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupController.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @param requestId 参数 请求Id；parameter request id。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:groups:write','CAP_*')")
    public GatewayGroupService.GatewayGroupView create(
            @Valid @RequestBody CreateRequest request,
            AdminActor actor,
            @RequestHeader(value = "X-Request-Id",
                    required = false) String requestId) {
        return service.create(
                new GatewayGroupService.CreateGatewayGroup(
                        request.gatewayGroupCode(),
                        request.displayName(),
                        request.env(),
                        request.namespace(),
                        request.description()
                ),
                actor,
                audit(requestId)
        );
    }

    /**
     * 中文说明：执行 get 操作；该方法是 {@code GatewayGroupController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get operation; this method is the invocation entry point on {@code GatewayGroupController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupController.get(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 get 的处理结果；returns the result of the operation.
     */
    @GetMapping("/{id}")
    public GatewayGroupService.GatewayGroupView get(
            @PathVariable String id) {
        return service.get(id);
    }

    /**
     * 中文说明：执行 update 操作；该方法是 {@code GatewayGroupController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update operation; this method is the invocation entry point on {@code GatewayGroupController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupController.update(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @param requestId 参数 请求Id；parameter request id。
     * @return 返回 update 的处理结果；returns the result of the operation.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:groups:write','CAP_*')")
    public GatewayGroupService.GatewayGroupView update(
            @PathVariable String id,
            @Valid @RequestBody UpdateRequest request,
            AdminActor actor,
            @RequestHeader(value = "X-Request-Id",
                    required = false) String requestId) {
        return service.update(
                id,
                new GatewayGroupService.UpdateGatewayGroup(
                        request.displayName(),
                        request.description(),
                        request.expectedRevision()
                ),
                actor,
                audit(requestId)
        );
    }

    /**
     * 中文说明：执行 enable 操作；该方法是 {@code GatewayGroupController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the enable operation; this method is the invocation entry point on {@code GatewayGroupController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupController.enable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 enable 的处理结果；returns the result of the operation.
     */
    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:groups:write','CAP_*')")
    public GatewayGroupService.GatewayGroupView enable(
            @PathVariable String id,
            AdminActor actor) {
        return service.setEnabled(
                id,
                true,
                actor,
                audit(null)
        );
    }

    /**
     * 中文说明：执行 disable 操作；该方法是 {@code GatewayGroupController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the disable operation; this method is the invocation entry point on {@code GatewayGroupController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupController.disable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 disable 的处理结果；returns the result of the operation.
     */
    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:groups:write','CAP_*')")
    public GatewayGroupService.GatewayGroupView disable(
            @PathVariable String id,
            AdminActor actor) {
        return service.setEnabled(
                id,
                false,
                actor,
                audit(null)
        );
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code GatewayGroupController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code GatewayGroupController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupController.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param requestId 参数 请求Id；parameter request id。
     * @return 返回 审计 的处理结果；returns the result of the operation.
     */
    private RequestAuditContext audit(String requestId) {
        return RequestAuditContext.current(requestId);
    }

    /**
     * 中文说明：{@code CreateRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Create请求相关的职责与边界。
     * English summary: {@code CreateRequest} is an immutable data carrier in the current Gateway module; it owns the create request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupCode 参数 网关GroupCode；parameter gateway group code。
     * @param displayName 参数 displayName；parameter display name。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param description 参数 description；parameter description。
     */
    public record CreateRequest(
            /**
             * 中文说明：保存 网关GroupCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayGroupController.CreateRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group code; its type is {@code String}, and {@code GatewayGroupController.CreateRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayGroupController.CreateRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupController.CreateRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String gatewayGroupCode,
            /**
             * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayGroupController.CreateRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code GatewayGroupController.CreateRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayGroupController.CreateRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupController.CreateRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String displayName,
            /**
             * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayGroupController.CreateRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code GatewayGroupController.CreateRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayGroupController.CreateRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupController.CreateRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String env,
            /**
             * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayGroupController.CreateRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code GatewayGroupController.CreateRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayGroupController.CreateRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupController.CreateRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String namespace,
            /**
             * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayGroupController.CreateRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code GatewayGroupController.CreateRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayGroupController.CreateRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupController.CreateRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String description
    ) {
    }

    /**
     * 中文说明：{@code UpdateRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Update请求相关的职责与边界。
     * English summary: {@code UpdateRequest} is an immutable data carrier in the current Gateway module; it owns the update request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param displayName 参数 displayName；parameter display name。
     * @param description 参数 description；parameter description。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     */
    public record UpdateRequest(
            /**
             * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayGroupController.UpdateRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code GatewayGroupController.UpdateRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayGroupController.UpdateRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupController.UpdateRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String displayName,
            /**
             * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayGroupController.UpdateRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code GatewayGroupController.UpdateRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayGroupController.UpdateRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupController.UpdateRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String description,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayGroupController.UpdateRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code GatewayGroupController.UpdateRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayGroupController.UpdateRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupController.UpdateRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedRevision
    ) {
    }
}

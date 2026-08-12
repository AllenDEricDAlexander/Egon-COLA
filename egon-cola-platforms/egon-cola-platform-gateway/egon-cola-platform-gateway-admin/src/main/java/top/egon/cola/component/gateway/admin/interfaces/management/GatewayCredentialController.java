package top.egon.cola.component.gateway.admin.interfaces.management;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.application.credential.GatewayCredentialService;
import top.egon.cola.component.gateway.admin.domain.AdminActor;

import java.time.Duration;
import java.util.List;

/**
 * 中文说明：{@code GatewayCredentialController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责网关凭证控制器相关的职责与边界。
 * English summary: {@code GatewayCredentialController} is a gateway credential controller controller in the current Gateway module; it owns the gateway credential controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin/applications/{applicationId}/credentials")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
public class GatewayCredentialController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code GatewayCredentialService}，由 {@code GatewayCredentialController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code GatewayCredentialService}, and {@code GatewayCredentialController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCredentialController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCredentialService service;

    /**
     * 中文说明：创建 {@code GatewayCredentialController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayCredentialController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public GatewayCredentialController(GatewayCredentialService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 list 操作；该方法是 {@code GatewayCredentialController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code GatewayCredentialController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialController.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    @GetMapping
    public List<GatewayCredentialService.CredentialView> list(
            @PathVariable String applicationId) {
        return service.list(applicationId);
    }

    /**
     * 中文说明：执行 create 操作；该方法是 {@code GatewayCredentialController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code GatewayCredentialController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialController.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:credentials:write','CAP_*')")
    public GatewayCredentialService.IssuedCredential create(
            @PathVariable String applicationId,
            AdminActor actor) {
        return service.create(applicationId, actor, audit());
    }

    /**
     * 中文说明：执行 rotate 操作；该方法是 {@code GatewayCredentialController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rotate operation; this method is the invocation entry point on {@code GatewayCredentialController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialController.rotate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param keyId 参数 键Id；parameter key id。
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 rotate 的处理结果；returns the result of the operation.
     */
    @PostMapping("/{keyId}/rotate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:credentials:write','CAP_*')")
    public GatewayCredentialService.IssuedCredential rotate(
            @PathVariable String applicationId,
            @PathVariable String keyId,
            @Valid @RequestBody RotateRequest request,
            AdminActor actor) {
        return service.rotate(
                applicationId,
                keyId,
                Duration.ofMinutes(request.overlapMinutes()),
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 revoke 操作；该方法是 {@code GatewayCredentialController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revoke operation; this method is the invocation entry point on {@code GatewayCredentialController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialController.revoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param keyId 参数 键Id；parameter key id。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 revoke 的处理结果；returns the result of the operation.
     */
    @PostMapping("/{keyId}/revoke")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:credentials:write','CAP_*')")
    public GatewayCredentialService.CredentialView revoke(
            @PathVariable String applicationId,
            @PathVariable String keyId,
            AdminActor actor) {
        return service.revoke(
                applicationId,
                keyId,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code GatewayCredentialController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code GatewayCredentialController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialController.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 审计 的处理结果；returns the result of the operation.
     */
    private RequestAuditContext audit() {
        return RequestAuditContext.current();
    }

    /**
     * 中文说明：{@code RotateRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Rotate请求相关的职责与边界。
     * English summary: {@code RotateRequest} is an immutable data carrier in the current Gateway module; it owns the rotate request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param overlapMinutes 参数 overlapMinutes；parameter overlap minutes。
     */
    public record RotateRequest(
            /**
             * 中文说明：保存 overlapMinutes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayCredentialController.RotateRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by overlap minutes; its type is {@code long}, and {@code GatewayCredentialController.RotateRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialController.RotateRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialController.RotateRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @Min(0) @Max(1440) long overlapMinutes
    ) {
    }
}

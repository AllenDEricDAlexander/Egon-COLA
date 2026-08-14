package top.egon.cola.component.gateway.admin.mcp.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactRequestDTO;
import top.egon.cola.component.gateway.admin.mcp.service.McpControlPlaneService;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 中文说明：{@code McpAppAdminController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责MCPApp管理端控制器相关的职责与边界。
 * English summary: {@code McpAppAdminController} is a mcp app admin controller controller in the current Gateway module; it owns the mcp app admin controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin/mcp/apps")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:read','CAP_*')")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "gateway-admin",
        entityDomainName = "Gateway Admin 管理实体域",
        code = "gateway-admin-mcp-app-admin-controller",
        name = "McpAppAdminController 管理接口组")
public class McpAppAdminController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code McpControlPlaneService}，由 {@code McpAppAdminController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code McpControlPlaneService}, and {@code McpAppAdminController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpAppAdminController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppAdminController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpControlPlaneService service;

    /**
     * 中文说明：创建 {@code McpAppAdminController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpAppAdminController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public McpAppAdminController(McpControlPlaneService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 list 操作；该方法是 {@code McpAppAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code McpAppAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppAdminController.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @GetMapping("/artifacts")
    public List<top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO> list(
            @RequestParam String gatewayGroupId) {
        return service.artifacts(gatewayGroupId);
    }

    /**
     * 中文说明：执行 register 操作；该方法是 {@code McpAppAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the register operation; this method is the invocation entry point on {@code McpAppAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppAdminController.register(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 register 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @PostMapping("/artifacts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO register(
            @Valid @RequestBody McpArtifactRequestDTO request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.registerArtifact(
                request.mutation(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    /**
     * 中文说明：执行 upload 操作；该方法是 {@code McpAppAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the upload operation; this method is the invocation entry point on {@code McpAppAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppAdminController.upload(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param appCode 参数 appCode；parameter app code。
     * @param version 参数 version；parameter version。
     * @param displayName 参数 displayName；parameter display name。
     * @param resourceUri 参数 资源Uri；parameter resource uri。
     * @param mimeType 参数 mimeType；parameter mime type。
     * @param contentSecurityPolicy 参数 content安全策略；parameter content security policy。
     * @param permissions 参数 permissions；parameter permissions。
     * @param allowedOrigins 参数 allowedOrigins；parameter allowed origins。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     * @param artifact 参数 制品；parameter artifact。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 upload 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @PostMapping(
            value = "/artifacts/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO upload(
            @RequestParam @NotBlank String gatewayGroupId,
            @RequestParam @NotBlank String appCode,
            @RequestParam @NotBlank String version,
            @RequestParam @NotBlank String displayName,
            @RequestParam @NotBlank String resourceUri,
            @RequestParam @NotBlank String mimeType,
            @RequestParam @NotBlank String contentSecurityPolicy,
            @RequestParam @NotEmpty Set<String> permissions,
            @RequestParam(required = false) Set<String> allowedOrigins,
            @RequestParam @PositiveOrZero long expectedRevision,
            @RequestParam @PositiveOrZero long expectedDraftRevision,
            @RequestParam @NotBlank String changeReason,
            @RequestPart("artifact") MultipartFile artifact,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) throws IOException {
        return service.uploadArtifact(
                new top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO(
                        gatewayGroupId,
                        appCode,
                        version,
                        displayName,
                        resourceUri,
                        mimeType,
                        contentSecurityPolicy,
                        permissions,
                        allowedOrigins == null ? Set.of() : allowedOrigins,
                        artifact.getBytes(),
                        expectedRevision,
                        expectedDraftRevision,
                        changeReason
                ),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    /**
     * 中文说明：执行 get 操作；该方法是 {@code McpAppAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get operation; this method is the invocation entry point on {@code McpAppAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppAdminController.get(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 get 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @GetMapping("/artifacts/{id}")
    public top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO get(
            @PathVariable String id) {
        return service.artifact(id);
    }

    /**
     * 中文说明：执行 revoke 操作；该方法是 {@code McpAppAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revoke operation; this method is the invocation entry point on {@code McpAppAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppAdminController.revoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 revoke 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @DeleteMapping("/artifacts/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO revoke(
            @PathVariable String id,
            @Valid @RequestBody top.egon.cola.component.gateway.admin.mcp.domain.dto.McpServerMutationRequestDTO request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.revokeArtifact(
                id,
                new top.egon.cola.component.gateway.admin.mcp.domain.dto.McpMutationControlDTO(
                        request.gatewayGroupId(),
                        request.expectedRevision(),
                        request.expectedDraftRevision(),
                        request.changeReason()
                ),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }


}

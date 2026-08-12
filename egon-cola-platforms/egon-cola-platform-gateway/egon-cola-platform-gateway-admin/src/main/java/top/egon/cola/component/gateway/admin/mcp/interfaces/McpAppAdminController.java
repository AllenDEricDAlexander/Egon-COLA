package top.egon.cola.component.gateway.admin.mcp.interfaces;

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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.mcp.application.McpControlPlaneService;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpArtifactMetadataStore;

import java.util.List;
import java.io.IOException;
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
    @GetMapping("/artifacts")
    public List<JdbcMcpArtifactMetadataStore.ArtifactMetadata> list(
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
    @PostMapping("/artifacts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult register(
            @Valid @RequestBody ArtifactRequest request,
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
    @PostMapping(
            value = "/artifacts/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult upload(
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
                new McpControlPlaneService.ArtifactUpload(
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
    @GetMapping("/artifacts/{id}")
    public JdbcMcpArtifactMetadataStore.ArtifactMetadata get(
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
    @DeleteMapping("/artifacts/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public McpControlPlaneService.MutationResult revoke(
            @PathVariable String id,
            @Valid @RequestBody McpServerController.MutationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.revokeArtifact(
                id,
                new McpControlPlaneService.MutationControl(
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

    /**
     * 中文说明：{@code ArtifactRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责制品请求相关的职责与边界。
     * English summary: {@code ArtifactRequest} is an immutable data carrier in the current Gateway module; it owns the artifact request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param appCode 参数 appCode；parameter app code。
     * @param version 参数 version；parameter version。
     * @param displayName 参数 displayName；parameter display name。
     * @param resourceUri 参数 资源Uri；parameter resource uri。
     * @param artifactReference 参数 制品Reference；parameter artifact reference。
     * @param sha256 参数 sha256；parameter sha256。
     * @param sizeBytes 参数 sizeBytes；parameter size bytes。
     * @param mimeType 参数 mimeType；parameter mime type。
     * @param contentSecurityPolicy 参数 content安全策略；parameter content security policy。
     * @param permissions 参数 permissions；parameter permissions。
     * @param allowedOrigins 参数 allowedOrigins；parameter allowed origins。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record ArtifactRequest(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpAppAdminController.ArtifactRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpAppAdminController.ArtifactRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppAdminController.ArtifactRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppAdminController.ArtifactRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String gatewayGroupId,
            /**
             * 中文说明：保存 appCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpAppAdminController.ArtifactRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by app code; its type is {@code String}, and {@code McpAppAdminController.ArtifactRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppAdminController.ArtifactRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppAdminController.ArtifactRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String appCode,
            /**
             * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpAppAdminController.ArtifactRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code McpAppAdminController.ArtifactRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppAdminController.ArtifactRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppAdminController.ArtifactRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String version,
            /**
             * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpAppAdminController.ArtifactRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code McpAppAdminController.ArtifactRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppAdminController.ArtifactRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppAdminController.ArtifactRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String displayName,
            /**
             * 中文说明：保存 资源Uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpAppAdminController.ArtifactRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by resource uri; its type is {@code String}, and {@code McpAppAdminController.ArtifactRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppAdminController.ArtifactRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppAdminController.ArtifactRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String resourceUri,
            /**
             * 中文说明：保存 制品Reference 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpAppAdminController.ArtifactRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by artifact reference; its type is {@code String}, and {@code McpAppAdminController.ArtifactRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppAdminController.ArtifactRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppAdminController.ArtifactRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String artifactReference,
            /**
             * 中文说明：保存 sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpAppAdminController.ArtifactRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by sha256; its type is {@code String}, and {@code McpAppAdminController.ArtifactRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppAdminController.ArtifactRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppAdminController.ArtifactRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String sha256,
            /**
             * 中文说明：保存 sizeBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpAppAdminController.ArtifactRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by size bytes; its type is {@code long}, and {@code McpAppAdminController.ArtifactRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppAdminController.ArtifactRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppAdminController.ArtifactRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long sizeBytes,
            /**
             * 中文说明：保存 mimeType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpAppAdminController.ArtifactRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by mime type; its type is {@code String}, and {@code McpAppAdminController.ArtifactRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppAdminController.ArtifactRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppAdminController.ArtifactRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String mimeType,
            /**
             * 中文说明：保存 content安全策略 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpAppAdminController.ArtifactRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content security policy; its type is {@code String}, and {@code McpAppAdminController.ArtifactRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppAdminController.ArtifactRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppAdminController.ArtifactRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String contentSecurityPolicy,
            /**
             * 中文说明：保存 permissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpAppAdminController.ArtifactRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by permissions; its type is {@code Set<String>}, and {@code McpAppAdminController.ArtifactRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppAdminController.ArtifactRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppAdminController.ArtifactRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotEmpty Set<String> permissions,
            /**
             * 中文说明：保存 allowedOrigins 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpAppAdminController.ArtifactRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by allowed origins; its type is {@code Set<String>}, and {@code McpAppAdminController.ArtifactRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppAdminController.ArtifactRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppAdminController.ArtifactRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> allowedOrigins,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpAppAdminController.ArtifactRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpAppAdminController.ArtifactRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppAdminController.ArtifactRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppAdminController.ArtifactRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpAppAdminController.ArtifactRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpAppAdminController.ArtifactRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppAdminController.ArtifactRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppAdminController.ArtifactRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @PositiveOrZero long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpAppAdminController.ArtifactRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpAppAdminController.ArtifactRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppAdminController.ArtifactRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppAdminController.ArtifactRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String changeReason
    ) {

        /**
         * 中文说明：执行 mutation 操作；该方法是 {@code McpAppAdminController.ArtifactRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the mutation operation; this method is the invocation entry point on {@code McpAppAdminController.ArtifactRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpAppAdminController.ArtifactRequest.mutation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 mutation 的处理结果；returns the result of the operation.
         */
        private McpControlPlaneService.ArtifactMutation mutation() {
            return new McpControlPlaneService.ArtifactMutation(
                    gatewayGroupId,
                    appCode,
                    version,
                    displayName,
                    resourceUri,
                    artifactReference,
                    sha256,
                    sizeBytes,
                    mimeType,
                    contentSecurityPolicy,
                    permissions,
                    allowedOrigins == null ? Set.of() : allowedOrigins,
                    expectedRevision,
                    expectedDraftRevision,
                    changeReason
            );
        }
    }
}

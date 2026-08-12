package top.egon.cola.component.gateway.admin.mcp.interfaces;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.mcp.application.McpControlPlaneService;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 中文说明：{@code McpProtocolInspectorController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责MCPProtocolInspector控制器相关的职责与边界。
 * English summary: {@code McpProtocolInspectorController} is a mcp protocol inspector controller controller in the current Gateway module; it owns the mcp protocol inspector controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin/mcp/servers")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:test','CAP_*')")
public class McpProtocolInspectorController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code McpControlPlaneService}，由 {@code McpProtocolInspectorController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code McpControlPlaneService}, and {@code McpProtocolInspectorController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpProtocolInspectorController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpProtocolInspectorController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpControlPlaneService service;

    /**
     * 中文说明：创建 {@code McpProtocolInspectorController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpProtocolInspectorController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public McpProtocolInspectorController(McpControlPlaneService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 inspect 操作；该方法是 {@code McpProtocolInspectorController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the inspect operation; this method is the invocation entry point on {@code McpProtocolInspectorController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpProtocolInspectorController.inspect(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverId 参数 服务器Id；parameter server id。
     * @param request 参数 请求；parameter request。
     * @return 返回 inspect 的处理结果；returns the result of the operation.
     */
    @PostMapping("/{serverId}/protocol-inspect")
    public Inspection inspect(
            @PathVariable String serverId,
            @Valid @RequestBody InspectRequest request) {
        var server = service.getServer(serverId);
        McpProtocolDialect dialect = McpProtocolDialect.valueOf(
                request.dialect()
        );
        if (!server.dialects().contains(dialect.name())) {
            throw new IllegalArgumentException(
                    "MCP protocol dialect is not enabled for this Server"
            );
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("MCP-Protocol-Version", dialect.protocolVersion());
        if (dialect.releaseCandidate()) {
            headers.put("MCP-Method", request.method());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("id", "inspect-1");
        body.put("method", request.method());
        body.put("params", request.params());
        return new Inspection(
                "/mcp/" + server.serverCode(),
                Map.copyOf(headers),
                Map.copyOf(body),
                dialect.releaseCandidate()
        );
    }

    /**
     * 中文说明：{@code InspectRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Inspect请求相关的职责与边界。
     * English summary: {@code InspectRequest} is an immutable data carrier in the current Gateway module; it owns the inspect request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param dialect 参数 dialect；parameter dialect。
     * @param method 参数 方法；parameter method。
     * @param params 参数 params；parameter params。
     */
    public record InspectRequest(
            /**
             * 中文说明：保存 dialect 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpProtocolInspectorController.InspectRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by dialect; its type is {@code String}, and {@code McpProtocolInspectorController.InspectRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpProtocolInspectorController.InspectRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpProtocolInspectorController.InspectRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String dialect,
            /**
             * 中文说明：保存 方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpProtocolInspectorController.InspectRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by method; its type is {@code String}, and {@code McpProtocolInspectorController.InspectRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpProtocolInspectorController.InspectRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpProtocolInspectorController.InspectRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String method,
            /**
             * 中文说明：保存 params 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpProtocolInspectorController.InspectRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by params; its type is {@code Map<String, Object>}, and {@code McpProtocolInspectorController.InspectRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpProtocolInspectorController.InspectRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpProtocolInspectorController.InspectRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotNull Map<String, Object> params
    ) {
    }

    /**
     * 中文说明：{@code Inspection} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Inspection相关的职责与边界。
     * English summary: {@code Inspection} is an immutable data carrier in the current Gateway module; it owns the inspection-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param path 参数 path；parameter path。
     * @param headers 参数 headers；parameter headers。
     * @param body 参数 body；parameter body。
     * @param releaseCandidate 参数 发布Candidate；parameter release candidate。
     */
    public record Inspection(
            /**
             * 中文说明：保存 path 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpProtocolInspectorController.Inspection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by path; its type is {@code String}, and {@code McpProtocolInspectorController.Inspection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpProtocolInspectorController.Inspection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpProtocolInspectorController.Inspection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String path,
            /**
             * 中文说明：保存 headers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code McpProtocolInspectorController.Inspection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by headers; its type is {@code Map<String, String>}, and {@code McpProtocolInspectorController.Inspection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpProtocolInspectorController.Inspection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpProtocolInspectorController.Inspection}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, String> headers,
            /**
             * 中文说明：保存 body 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpProtocolInspectorController.Inspection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by body; its type is {@code Map<String, Object>}, and {@code McpProtocolInspectorController.Inspection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpProtocolInspectorController.Inspection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpProtocolInspectorController.Inspection}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> body,
            /**
             * 中文说明：保存 发布Candidate 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpProtocolInspectorController.Inspection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by release candidate; its type is {@code boolean}, and {@code McpProtocolInspectorController.Inspection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpProtocolInspectorController.Inspection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpProtocolInspectorController.Inspection}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean releaseCandidate
    ) {
    }
}

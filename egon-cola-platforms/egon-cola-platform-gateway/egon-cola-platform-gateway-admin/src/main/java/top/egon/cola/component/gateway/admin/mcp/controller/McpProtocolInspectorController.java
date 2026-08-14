package top.egon.cola.component.gateway.admin.mcp.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpProtocolInspectRequestDTO;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.McpProtocolInspectionVO;
import top.egon.cola.component.gateway.admin.mcp.service.McpControlPlaneService;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

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
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "gateway-admin",
        entityDomainName = "Gateway Admin 管理实体域",
        code = "gateway-admin-mcp-protocol-inspector-controller",
        name = "McpProtocolInspectorController 管理接口组")
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
    @GatewayOperation(externalAccessible = true)
    @PostMapping("/{serverId}/protocol-inspect")
    public McpProtocolInspectionVO inspect(
            @PathVariable String serverId,
            @Valid @RequestBody McpProtocolInspectRequestDTO request) {
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
        return new McpProtocolInspectionVO(
                "/mcp/" + server.serverCode(),
                Map.copyOf(headers),
                Map.copyOf(body),
                dialect.releaseCandidate()
        );
    }




}

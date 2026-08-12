package top.egon.cola.component.gateway.admin.mcp.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code McpServerRepository} 是接口契约，位于当前 Gateway 模块的相关包中，负责MCP服务器Repository相关的职责与边界。
 * English summary: {@code McpServerRepository} is an interface contract in the current Gateway module; it owns the mcp server repository-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface McpServerRepository
        extends JpaRepository<McpServerEntity, String> {

    /**
     * 中文说明：执行 findAllBy网关GroupIdAndDeletedFalseOrderBy服务器Code 操作；该方法是 {@code McpServerRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find all by gateway group id and deleted false order by server code operation; this method is the invocation entry point on {@code McpServerRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerRepository.findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 findAllBy网关GroupIdAndDeletedFalseOrderBy服务器Code 的处理结果；returns the result of the operation.
     */
    List<McpServerEntity> findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode(
            String gatewayGroupId);

    /**
     * 中文说明：执行 findBy网关GroupIdAnd服务器CodeAndDeletedFalse 操作；该方法是 {@code McpServerRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find by gateway group id and server code and deleted false operation; this method is the invocation entry point on {@code McpServerRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerRepository.findByGatewayGroupIdAndServerCodeAndDeletedFalse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @return 返回 findBy网关GroupIdAnd服务器CodeAndDeletedFalse 的处理结果；returns the result of the operation.
     */
    Optional<McpServerEntity> findByGatewayGroupIdAndServerCodeAndDeletedFalse(
            String gatewayGroupId,
            String serverCode);

    /**
     * 中文说明：执行 findByIdAndDeletedFalse 操作；该方法是 {@code McpServerRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find by id and deleted false operation; this method is the invocation entry point on {@code McpServerRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerRepository.findByIdAndDeletedFalse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 findByIdAndDeletedFalse 的处理结果；returns the result of the operation.
     */
    Optional<McpServerEntity> findByIdAndDeletedFalse(String id);
}

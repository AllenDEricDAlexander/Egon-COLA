package top.egon.cola.component.yuheng.admin.mcp.repository;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpArtifactMetadataBO;

/**
 * 中文说明：{@code McpArtifactMetadataRepository} 是持久化公开业务端口，签名与原 {@code legacy JDBC ArtifactMetadata facade} 一致，仅把 PO 返回与入参替换为 BO。
 * English summary: {@code McpArtifactMetadataRepository} is the public persistence port whose signatures mirror {@code legacy JDBC ArtifactMetadata facade} with PO types replaced by BO types.
 *
 * 用法 / Usage: 由应用/领域服务按限定名注入该端口，实现类承担事务与守卫查询。/ Inject this port from the application layer; the implementation owns transactions and guarded queries.
 */
@Validated
public interface McpArtifactMetadataRepository {

    /**
     * 中文说明：执行 save 操作；该端口方法是 McpArtifactMetadataRepository 的调用入口。
     * English summary: Executes the save operation; this port method is the invocation entry point of McpArtifactMetadataRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpArtifactMetadataRepository.save(...)}。
     * @param artifact 参数；parameter.
     */
    void save(McpArtifactMetadataBO artifact);

    /**
     * 中文说明：执行 find 操作；该端口方法是 McpArtifactMetadataRepository 的调用入口。
     * English summary: Executes the find operation; this port method is the invocation entry point of McpArtifactMetadataRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpArtifactMetadataRepository.find(...)}。
     * @param id 参数；parameter.
     * @return 返回 find 的处理结果；returns the result.
     */
    Optional<McpArtifactMetadataBO> find(String id);

    /**
     * 中文说明：执行 list 操作；该端口方法是 McpArtifactMetadataRepository 的调用入口。
     * English summary: Executes the list operation; this port method is the invocation entry point of McpArtifactMetadataRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpArtifactMetadataRepository.list(...)}。
     * @param gatewayGroupId 参数；parameter.
     * @return 返回 list 的处理结果；returns the result.
     */
    List<McpArtifactMetadataBO> list(String gatewayGroupId);

    /**
     * 中文说明：执行 revoke 操作；该端口方法是 McpArtifactMetadataRepository 的调用入口。
     * English summary: Executes the revoke operation; this port method is the invocation entry point of McpArtifactMetadataRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpArtifactMetadataRepository.revoke(...)}。
     * @param id 参数；parameter.
     * @return 返回 revoke 的处理结果；returns the result.
     */
    boolean revoke(String id);

}

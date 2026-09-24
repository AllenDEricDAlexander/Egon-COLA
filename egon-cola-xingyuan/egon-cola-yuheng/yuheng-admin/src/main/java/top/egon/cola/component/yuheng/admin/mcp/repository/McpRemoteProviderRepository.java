package top.egon.cola.component.yuheng.admin.mcp.repository;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpRemoteCapabilityBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpRemoteMountDraftBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpRemoteProviderDraftBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.dto.McpRemoteProviderDraftMutationDTO;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;

/**
 * 中文说明：{@code McpRemoteProviderRepository} 是持久化公开业务端口，签名与原 {@code legacy JDBC RemoteProvider facade} 一致，仅把 PO 返回与入参替换为 BO。
 * English summary: {@code McpRemoteProviderRepository} is the public persistence port whose signatures mirror {@code legacy JDBC RemoteProvider facade} with PO types replaced by BO types.
 *
 * 用法 / Usage: 由应用/领域服务按限定名注入该端口，实现类承担事务与守卫查询。/ Inject this port from the application layer; the implementation owns transactions and guarded queries.
 */
@Validated
public interface McpRemoteProviderRepository {

    /**
     * 中文说明：执行 providers 操作；该端口方法是 McpRemoteProviderRepository 的调用入口。
     * English summary: Executes the providers operation; this port method is the invocation entry point of McpRemoteProviderRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderRepository.providers(...)}。
     * @param gatewayGroupId 参数；parameter.
     * @return 返回 providers 的处理结果；returns the result.
     */
    List<McpRemoteProviderDraftBO> providers(String gatewayGroupId);

    /**
     * 中文说明：执行 saveProvider 操作；该端口方法是 McpRemoteProviderRepository 的调用入口。
     * English summary: Executes the saveProvider operation; this port method is the invocation entry point of McpRemoteProviderRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderRepository.saveProvider(..., ..., ..., ...)}。
     * @param provider 参数；parameter.
     * @return 返回 saveProvider 的处理结果；returns the result.
     */
    McpRemoteProviderDraftMutationDTO saveProvider(McpRemoteProviderDraftBO provider, long expectedRevision, AdminActor actor, Instant now);

    /**
     * 中文说明：执行 capabilities 操作；该端口方法是 McpRemoteProviderRepository 的调用入口。
     * English summary: Executes the capabilities operation; this port method is the invocation entry point of McpRemoteProviderRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderRepository.capabilities(...)}。
     * @param providerId 参数；parameter.
     * @return 返回 capabilities 的处理结果；returns the result.
     */
    List<McpRemoteCapabilityBO> capabilities(String providerId);

    /**
     * 中文说明：执行 replaceCapabilities 操作；该端口方法是 McpRemoteProviderRepository 的调用入口。
     * English summary: Executes the replaceCapabilities operation; this port method is the invocation entry point of McpRemoteProviderRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderRepository.replaceCapabilities(..., ..., ..., ...)}。
     * @param providerId 参数；parameter.
     */
    void replaceCapabilities(String providerId, String fingerprint, List<McpRemoteCapabilityBO> capabilities, Instant syncedAt);

    /**
     * 中文说明：执行 mounts 操作；该端口方法是 McpRemoteProviderRepository 的调用入口。
     * English summary: Executes the mounts operation; this port method is the invocation entry point of McpRemoteProviderRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderRepository.mounts(...)}。
     * @param gatewayGroupId 参数；parameter.
     * @return 返回 mounts 的处理结果；returns the result.
     */
    List<McpRemoteMountDraftBO> mounts(String gatewayGroupId);

    /**
     * 中文说明：执行 saveMount 操作；该端口方法是 McpRemoteProviderRepository 的调用入口。
     * English summary: Executes the saveMount operation; this port method is the invocation entry point of McpRemoteProviderRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderRepository.saveMount(..., ..., ..., ...)}。
     * @param mount 参数；parameter.
     * @return 返回 saveMount 的处理结果；returns the result.
     */
    McpRemoteProviderDraftMutationDTO saveMount(McpRemoteMountDraftBO mount, long expectedRevision, AdminActor actor, Instant now);

    /**
     * 中文说明：执行 softDeleteProvider 操作；该端口方法是 McpRemoteProviderRepository 的调用入口。
     * English summary: Executes the softDeleteProvider operation; this port method is the invocation entry point of McpRemoteProviderRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderRepository.softDeleteProvider(..., ..., ..., ...)}。
     * @param id 参数；parameter.
     * @return 返回 softDeleteProvider 的处理结果；returns the result.
     */
    McpRemoteProviderDraftMutationDTO softDeleteProvider(String id, long expectedRevision, AdminActor actor, Instant now);

    /**
     * 中文说明：执行 softDeleteMount 操作；该端口方法是 McpRemoteProviderRepository 的调用入口。
     * English summary: Executes the softDeleteMount operation; this port method is the invocation entry point of McpRemoteProviderRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderRepository.softDeleteMount(..., ..., ..., ...)}。
     * @param id 参数；parameter.
     * @return 返回 softDeleteMount 的处理结果；returns the result.
     */
    McpRemoteProviderDraftMutationDTO softDeleteMount(String id, long expectedRevision, AdminActor actor, Instant now);

}

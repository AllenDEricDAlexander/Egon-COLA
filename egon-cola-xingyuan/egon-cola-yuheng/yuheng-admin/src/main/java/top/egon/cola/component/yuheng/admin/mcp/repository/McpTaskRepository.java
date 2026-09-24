package top.egon.cola.component.yuheng.admin.mcp.repository;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpTaskBO;

/**
 * 中文说明：{@code McpTaskRepository} 是持久化公开业务端口，签名与原 {@code legacy JDBC Task facade} 一致，仅把 PO 返回与入参替换为 BO。
 * English summary: {@code McpTaskRepository} is the public persistence port whose signatures mirror {@code legacy JDBC Task facade} with PO types replaced by BO types.
 *
 * 用法 / Usage: 由应用/领域服务按限定名注入该端口，实现类承担事务与守卫查询。/ Inject this port from the application layer; the implementation owns transactions and guarded queries.
 */
@Validated
public interface McpTaskRepository {

    /**
     * 中文说明：执行 create 操作；该端口方法是 McpTaskRepository 的调用入口。
     * English summary: Executes the create operation; this port method is the invocation entry point of McpTaskRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskRepository.create(...)}。
     * @param task 参数；parameter.
     */
    void create(McpTaskBO task);

    /**
     * 中文说明：执行 find 操作；该端口方法是 McpTaskRepository 的调用入口。
     * English summary: Executes the find operation; this port method is the invocation entry point of McpTaskRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskRepository.find(...)}。
     * @param id 参数；parameter.
     * @return 返回 find 的处理结果；returns the result.
     */
    Optional<McpTaskBO> find(String id);

    /**
     * 中文说明：执行 list 操作；该端口方法是 McpTaskRepository 的调用入口。
     * English summary: Executes the list operation; this port method is the invocation entry point of McpTaskRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskRepository.list(..., ...)}。
     * @param tenantId 参数；parameter.
     * @return 返回 list 的处理结果；returns the result.
     */
    List<McpTaskBO> list(String tenantId, String clientId);

    /**
     * 中文说明：执行 claim 操作；该端口方法是 McpTaskRepository 的调用入口。
     * English summary: Executes the claim operation; this port method is the invocation entry point of McpTaskRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskRepository.claim(..., ..., ..., ..., ...)}。
     * @param id 参数；parameter.
     * @return 返回 claim 的处理结果；returns the result.
     */
    boolean claim(String id, String workerOwner, Instant now, Instant leaseUntil, long expectedRevision);

    /**
     * 中文说明：执行 transition 操作；该端口方法是 McpTaskRepository 的调用入口。
     * English summary: Executes the transition operation; this port method is the invocation entry point of McpTaskRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskRepository.transition(..., ..., ..., ..., ..., ..., ..., ..., ...)}。
     * @param id 参数；parameter.
     * @return 返回 transition 的处理结果；returns the result.
     */
    boolean transition(String id, String currentState, String targetState, Map<String, Object> resultPayload, Map<String, Object> errorPayload, long expectedRevision, Instant now);

    /**
     * 中文说明：执行 cancel 操作；该端口方法是 McpTaskRepository 的调用入口。
     * English summary: Executes the cancel operation; this port method is the invocation entry point of McpTaskRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskRepository.cancel(..., ..., ...)}。
     * @param id 参数；parameter.
     * @return 返回 cancel 的处理结果；returns the result.
     */
    boolean cancel(String id, long expectedRevision, Instant now);


}

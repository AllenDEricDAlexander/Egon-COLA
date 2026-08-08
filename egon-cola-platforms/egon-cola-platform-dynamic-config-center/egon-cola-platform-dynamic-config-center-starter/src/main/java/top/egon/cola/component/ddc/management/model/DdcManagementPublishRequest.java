package top.egon.cola.component.ddc.management.model;

/**
 * 发布指定作用域配置的管理请求。 / Management request for publishing a scoped configuration.
 *
 * @param bizCode         业务编码 / business code
 * @param env             环境编码 / environment code
 * @param appCode         应用编码 / application code
 * @param configValue     待发布的配置值 / configuration value to publish
 * @param expectedVersion 用于乐观并发控制的预期版本 / expected version for optimistic concurrency control
 * @param changeId        幂等发布变更标识 / idempotent publication change identifier
 * @param timeoutMs       发布等待超时毫秒数 / publication wait timeout in milliseconds
 * @param operator        操作人标识 / operator identifier
 */
public record DdcManagementPublishRequest(
        String bizCode,
        String env,
        String appCode,
        String configValue,
        Long expectedVersion,
        String changeId,
        Long timeoutMs,
        String operator
) {
}

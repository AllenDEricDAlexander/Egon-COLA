package top.egon.cola.component.ddc.management.model;

/**
 * 删除指定作用域配置的管理请求。 / Management request for deleting a scoped configuration.
 *
 * @param bizCode         业务编码 / business code
 * @param env             环境编码 / environment code
 * @param appCode         应用编码 / application code
 * @param expectedVersion 用于乐观并发控制的预期版本 / expected version for optimistic concurrency control
 * @param operator        操作人标识 / operator identifier
 * @param reason          删除原因 / deletion reason
 */
public record DdcManagementConfigDeleteRequest(
        String bizCode,
        String env,
        String appCode,
        Long expectedVersion,
        String operator,
        String reason
) {
}

package top.egon.cola.component.ddc.management.model;

/**
 * 新增或更新指定作用域配置的管理请求。 / Management request for creating or updating a scoped configuration.
 *
 * @param bizCode         业务编码 / business code
 * @param env             环境编码 / environment code
 * @param appCode         应用编码 / application code
 * @param configValue     待保存的配置值 / configuration value to persist
 * @param description     配置说明 / configuration description
 * @param expectedVersion 用于乐观并发控制的预期版本 / expected version for optimistic concurrency control
 * @param operator        操作人标识 / operator identifier
 */
public record DdcManagementConfigUpsertRequest(
        String bizCode,
        String env,
        String appCode,
        String configValue,
        String description,
        Long expectedVersion,
        String operator
) {
}

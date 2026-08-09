package top.egon.cola.component.ddc.model.management;

import java.time.Instant;

/**
 * DDC 管理接口返回的配置快照。 / Configuration snapshot returned by the DDC management API.
 *
 * @param bizCode     业务编码 / business code
 * @param env         环境编码 / environment code
 * @param appCode     应用编码 / application code
 * @param resourceName 配置资源名 / configuration resource name
 * @param content      配置内容 / configuration content
 * @param format       配置格式 / configuration format
 * @param version     配置版本 / configuration version
 * @param enabled     是否启用 / whether the configuration is enabled
 * @param deleted     是否已逻辑删除 / whether the configuration is logically deleted
 * @param updatedAt   最近更新时间 / most recent update time
 */
public record DdcManagementConfig(
        String bizCode,
        String env,
        String appCode,
        String resourceName,
        String content,
        String format,
        Long version,
        boolean enabled,
        boolean deleted,
        Instant updatedAt
) {
}
